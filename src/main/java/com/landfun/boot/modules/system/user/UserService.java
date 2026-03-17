package com.landfun.boot.modules.system.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landfun.boot.infrastructure.exception.BizException;
import com.landfun.boot.infrastructure.web.AuthContext;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.modules.system.dept.DeptFilter;
import com.landfun.boot.infrastructure.util.RedisHelper;
import com.landfun.boot.modules.system.user.dto.ChangePasswordInput;
import com.landfun.boot.modules.system.user.dto.ChangeSelfPasswordReq;
import com.landfun.boot.modules.system.user.dto.CreateUserInput;
import com.landfun.boot.modules.system.user.dto.UpdateUserInput;
import com.landfun.boot.modules.system.user.dto.UserSpecification;
import com.landfun.boot.modules.system.user.dto.UserView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final JSqlClient sqlClient;
    private final StringRedisTemplate redisTemplate;

    public PageResult<UserView> page(UserSpecification spec, Pageable pageable) {
        Page<UserView> page = sqlClient.createQuery(UserTable.$)
                .where(spec)
                .orderBy(SpringOrders.toOrders(UserTable.$, pageable.getSort()))
                .select(UserTable.$.fetch(UserView.class))
                .fetchPage(pageable.getPageNumber(), pageable.getPageSize());

        return PageResult.of(page);
    }

    @Transactional
    public UserView create(CreateUserInput input) {
        checkEmailUnique(input.getEmail(), null);
        String hashedPassword = cn.hutool.crypto.digest.BCrypt.hashpw(input.getPassword());
        input.setPassword(hashedPassword);
        SimpleSaveResult<User> result = sqlClient.getEntities().save(input);
        return sqlClient.findById(UserView.class, result.getModifiedEntity().id());
    }

    public UserView getById(long id) {
        return sqlClient.findById(UserView.class, id);
    }

    @Transactional
    public UserView update(UpdateUserInput input) {
        log.info("Updating user with input: {}", input);
        checkEmailUnique(input.getEmail(), input.getId());

        sqlClient.getEntities().saveCommand(input)
                .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.UPDATE_ONLY)
                .execute();

        // Evict permissions cache so that it re-fetches if roles changed
        redisTemplate.delete("user:permissions:" + input.getId());

        return sqlClient.findById(UserView.class, input.getId());
    }

    @Transactional
    public void changePassword(ChangePasswordInput input) {
        String hashedPassword = cn.hutool.crypto.digest.BCrypt.hashpw(input.getPassword());
        sqlClient.createUpdate(UserTable.$)
                .set(UserTable.$.password(), hashedPassword)
                .where(UserTable.$.id().eq(input.getId()))
                .execute();
    }

    @Transactional
    public UserView updateSelf(String username) {
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId == null) {
            throw new BizException(401, "用户未登录");
        }
        if (username == null || username.isBlank() || username.trim().length() < 2) {
            throw new BizException(400, "用户名至少2个字符");
        }
        sqlClient.createUpdate(UserTable.$)
                .set(UserTable.$.username(), username.trim())
                .where(UserTable.$.id().eq(currentUserId))
                .execute();
        return sqlClient.findById(UserView.class, currentUserId);
    }

    @Transactional
    public void changeSelfPassword(ChangeSelfPasswordReq input) {
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId == null) {
            throw new BizException(401, "User not authenticated");
        }

        // Verify old password
        String currentPassword = sqlClient
                .filters(cfg -> cfg.disableByTypes(UserFilter.class, DeptFilter.class))
                .createQuery(UserTable.$)
                .where(UserTable.$.id().eq(currentUserId))
                .select(UserTable.$.password())
                .fetchOneOrNull();
        if (currentPassword == null) {
            throw new BizException(404, "用户不存在");
        }
        if (!cn.hutool.crypto.digest.BCrypt.checkpw(input.oldPassword(), currentPassword)) {
            throw new BizException(400, "当前密码不正确");
        }

        String hashedPassword = cn.hutool.crypto.digest.BCrypt.hashpw(input.password());
        sqlClient.createUpdate(UserTable.$)
                .set(UserTable.$.password(), hashedPassword)
                .where(UserTable.$.id().eq(currentUserId))
                .execute();
    }

    @Transactional
    public void delete(long id) {
        // 禁止删除超级管理员用户
        Boolean isSuperuser = sqlClient
                .filters(cfg -> cfg.disableByTypes(UserFilter.class, DeptFilter.class))
                .createQuery(UserTable.$)
                .where(
                        UserTable.$.id().eq(id),
                        UserTable.$.deleteTime().isNull())
                .select(UserTable.$.superuser())
                .fetchOneOrNull();
        if (Boolean.TRUE.equals(isSuperuser)) {
            throw new BizException("不能删除超级管理员用户");
        }

        sqlClient.deleteById(User.class, id);
        redisTemplate.delete("user:permissions:" + id);
        redisTemplate.delete("user:token:" + id);
    }

    /** 查询所有在线用户（Redis 中持有 token 的用户） */
    public List<OnlineUserVO> listOnlineUsers() {
        Set<String> keys = RedisHelper.scan(redisTemplate, "user:token:*");
        if (keys.isEmpty()) {
            return List.of();
        }

        List<OnlineUserVO> result = new ArrayList<>();
        for (String key : keys) {
            try {
                long userId = Long.parseLong(key.substring("user:token:".length()));
                User user = sqlClient.createQuery(UserTable.$)
                        .where(UserTable.$.id().eq(userId))
                        .select(UserTable.$)
                        .fetchOneOrNull();
                if (user != null) {
                    result.add(new OnlineUserVO(user.id(), user.username(), user.email(), user.isActive()));
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid token Redis key: {}", key);
            }
        }
        return result;
    }

    private void checkEmailUnique(String email, Long excludeUserId) {
        if (email == null || email.isBlank()) {
            return;
        }
        var q = sqlClient
                .filters(cfg -> cfg.disableByTypes(UserFilter.class, DeptFilter.class))
                .createQuery(UserTable.$)
                .where(UserTable.$.email().eq(email.trim()));
        if (excludeUserId != null) {
            q = q.where(UserTable.$.id().ne(excludeUserId));
        }
        long count = q.select(UserTable.$.id().count()).fetchOne();
        if (count > 0) {
            throw new BizException(400, "邮箱 " + email.trim() + " 已被其他用户使用，请更换");
        }
    }

    /** 踢出指定用户（删除 Redis token 和权限缓存） */
    public void kickout(long userId) {
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId != null && currentUserId == userId) {
            throw new BizException(400, "不能踢出自己");
        }
        redisTemplate.delete("user:token:" + userId);
        redisTemplate.delete("user:permissions:" + userId);
        log.info("User {} has been kicked out by user {}", userId, currentUserId);
    }
}
