package com.landfun.boot.modules.system.role;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landfun.boot.infrastructure.exception.BizException;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.modules.system.role.dto.CreateRoleInput;
import com.landfun.boot.modules.system.role.dto.UpdateRoleInput;
import com.landfun.boot.modules.system.role.dto.RoleSpecification;
import com.landfun.boot.modules.system.role.dto.RoleView;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.landfun.boot.modules.system.user.UserTable;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final JSqlClient sqlClient;
    private final StringRedisTemplate redisTemplate;

    public PageResult<RoleView> page(RoleSpecification spec, Pageable pageable) {
        Page<RoleView> page = sqlClient.createQuery(RoleTable.$)
                .where(spec)
                .orderBy(SpringOrders.toOrders(RoleTable.$, pageable.getSort()))
                .select(RoleTable.$.fetch(RoleView.class))
                .fetchPage(pageable.getPageNumber(), pageable.getPageSize());
        return PageResult.of(page);
    }

    @Transactional
    public RoleView create(CreateRoleInput input) {
        SimpleSaveResult<Role> result = sqlClient.getEntities().save(input);
        return sqlClient.findById(RoleView.class, result.getModifiedEntity().id());
    }

    @Transactional
    public RoleView update(UpdateRoleInput input) {
        sqlClient.getEntities().saveCommand(input)
                .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.UPDATE_ONLY)
                .execute();

        clearAllUserPermissionCaches();
        return sqlClient.findById(RoleView.class, input.getId());
    }

    @Transactional
    public void delete(long id) {
        long userCount = sqlClient
                .filters(cfg -> cfg.disableAll())
                .createQuery(UserTable.$)
                .where(
                        UserTable.$.roleId().eq(id),
                        UserTable.$.deleteTime().isNull())
                .select(UserTable.$.id().count())
                .fetchOne();
        if (userCount > 0) {
            throw new BizException("仍有用户绑定该角色，无法删除，请先调整这些用户的角色");
        }

        sqlClient.deleteById(Role.class, id);
        clearAllUserPermissionCaches();
    }

    private void clearAllUserPermissionCaches() {
        java.util.Set<String> keys = redisTemplate.keys("user:permissions:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public java.util.List<RoleView> listAll(RoleSpecification spec, Pageable pageable) {
        return sqlClient.createQuery(RoleTable.$)
                .where(spec)
                .orderBy(SpringOrders.toOrders(RoleTable.$, pageable.getSort()))
                .select(RoleTable.$.fetch(RoleView.class))
                .execute();
    }

    public RoleView findById(long id) {
        return sqlClient.findById(RoleView.class, id);
    }
}
