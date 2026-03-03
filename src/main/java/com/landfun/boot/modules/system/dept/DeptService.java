package com.landfun.boot.modules.system.dept;

import java.util.List;
import java.util.Set;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landfun.boot.infrastructure.exception.BizException;
import com.landfun.boot.infrastructure.service.DataScopeResolver;
import com.landfun.boot.infrastructure.service.DataScopeResolver.ScopeResult;
import com.landfun.boot.infrastructure.service.DataScopeService;
import com.landfun.boot.infrastructure.web.AuthContext;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.user.UserFilter;
import com.landfun.boot.modules.system.dept.dto.CreateDeptInput;
import com.landfun.boot.modules.system.dept.dto.UpdateDeptInput;
import com.landfun.boot.modules.system.dept.dto.DeptSpecification;
import com.landfun.boot.modules.system.dept.dto.DeptView;
import com.landfun.boot.modules.system.dept.dto.DeptTreeView;
import com.landfun.boot.modules.system.user.UserTable;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeptService {

    private final JSqlClient sqlClient;
    private final DataScopeService dataScopeService;

    public PageResult<DeptView> list(DeptSpecification spec, Pageable pageable) {
        Page<DeptView> page = sqlClient.createQuery(DeptTable.$)
                .where(spec)
                .orderBy(SpringOrders.toOrders(DeptTable.$, pageable.getSort()))
                .select(DeptTable.$.fetch(DeptView.class))
                .fetchPage(pageable.getPageNumber(), pageable.getPageSize());
        return PageResult.of(page);
    }

    public List<DeptTreeView> tree() {
        User user = AuthContext.getUser();
        ScopeResult scope = DataScopeResolver.resolve(user, dataScopeService::getSubDeptIds);

        if (scope.unrestricted()) {
            return sqlClient.createQuery(DeptTable.$)
                    .where(Predicate.or(
                            DeptTable.$.parentId().isNull(),
                            DeptTable.$.parentId().eq(0L)))
                    .select(DeptTable.$.fetch(DeptTreeView.class))
                    .execute();
        }

        if (scope.selfOnly()) {
            Long userDeptId = (user != null && user.dept() != null) ? user.dept().id() : null;
            if (userDeptId == null) {
                return List.of();
            }
            return sqlClient.createQuery(DeptTable.$)
                    .where(DeptTable.$.id().eq(userDeptId))
                    .select(DeptTable.$.fetch(DeptTreeView.class))
                    .execute();
        }

        Set<Long> allowed = scope.allowedDeptIds();
        return sqlClient.createQuery(DeptTable.$)
                .where(Predicate.or(
                        DeptTable.$.parentId().isNull(),
                        DeptTable.$.parentId().eq(0L),
                        DeptTable.$.parentId().notIn(allowed)))
                .select(DeptTable.$.fetch(DeptTreeView.class))
                .execute();
    }

    @Transactional
    public DeptView create(CreateDeptInput input) {
        SimpleSaveResult<Dept> result = sqlClient.getEntities().save(input);
        dataScopeService.evictSubDeptCache(null);
        return sqlClient.findById(DeptView.class, result.getModifiedEntity().id());
    }

    @Transactional
    public DeptView update(UpdateDeptInput input) {
        sqlClient.getEntities().saveCommand(input)
                .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.UPDATE_ONLY)
                .execute();
        dataScopeService.evictSubDeptCache(null);
        return sqlClient.findById(DeptView.class, input.getId());
    }

    @Transactional
    public void delete(long id) {
        // 1. 不允许删除仍有未删除子部门的部门
        long childCount = sqlClient
                .filters(cfg -> cfg.disableByTypes(UserFilter.class, DeptFilter.class))
                .createQuery(DeptTable.$)
                .where(
                        DeptTable.$.parentId().eq(id),
                        DeptTable.$.deleteTime().isNull())
                .select(DeptTable.$.id().count())
                .fetchOne();
        if (childCount > 0) {
            throw new BizException("当前部门仍包含子部门，无法删除，请先删除或迁移子部门");
        }

        // 2. 不允许删除仍有未删除用户挂靠的部门
        long userCount = sqlClient
                .filters(cfg -> cfg.disableByTypes(UserFilter.class, DeptFilter.class))
                .createQuery(UserTable.$)
                .where(
                        UserTable.$.deptId().eq(id),
                        UserTable.$.deleteTime().isNull())
                .select(UserTable.$.id().count())
                .fetchOne();
        if (userCount > 0) {
            throw new BizException("当前部门下仍有用户，无法删除，请先调整用户所属部门");
        }

        sqlClient.deleteById(Dept.class, id);
        dataScopeService.evictSubDeptCache(null);
    }
}
