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
import com.landfun.boot.modules.system.user.dto.ChangePasswordInput;
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
        String hashedPassword = cn.hutool.crypto.digest.BCrypt.hashpw(input.getPassword());
        input.setPassword(hashedPassword);
        SimpleSaveResult<User> result = sqlClient.getEntities().save(input);
        return sqlClient.findById(UserView.class, result.getModifiedEntity().id());
    }

    @Transactional
    public UserView update(UpdateUserInput input) {
        SimpleSaveResult<User> result = sqlClient.getEntities().save(input);
        return sqlClient.findById(UserView.class, result.getModifiedEntity().id());
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
    public void delete(long id) {
        sqlClient.deleteById(User.class, id);
    }

    /** 查询所有在线用户（Redis 中持有 token 的用户） */
    public List<OnlineUserVO> listOnlineUsers() {
        Set<String> keys = redisTemplate.keys("user:token:*");
        if (keys == null || keys.isEmpty()) {
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
