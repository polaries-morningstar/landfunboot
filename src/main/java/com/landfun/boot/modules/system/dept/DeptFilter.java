package com.landfun.boot.modules.system.dept;

import com.landfun.boot.infrastructure.web.AuthContext;
import com.landfun.boot.modules.system.role.DataScope;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.user.UserFetcher;
import com.landfun.boot.modules.system.user.UserTable;
import com.landfun.boot.modules.system.user.UserProps;
import com.landfun.boot.modules.system.dept.DeptProps;
import com.landfun.boot.modules.system.dept.DeptTable;
import com.landfun.boot.modules.system.dept.DeptFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeptFilter implements Filter<DeptProps> {

    private final JSqlClient sqlClient;

    @Override
    public void filter(FilterArgs<DeptProps> args) {
        User user = AuthContext.getUser();
        if (user == null) {
            return;
        }

        Long userId = user.id();
        boolean hasAll = false;
        boolean hasRecursive = false;
        boolean hasSame = false;

        if (org.babyfish.jimmer.ImmutableObjects.isLoaded(user, UserProps.ROLES) && user.roles() != null) {
            for (var role : user.roles()) {
                if (role.dataScope() == DataScope.ALL)
                    hasAll = true;
                if (role.dataScope() == DataScope.DEPT_RECURSIVE)
                    hasRecursive = true;
                if (role.dataScope() == DataScope.DEPT_SAME)
                    hasSame = true;
            }
        }

        if (hasAll) {
            return;
        }

        Long userDeptId = user.dept() == null ? null : user.dept().id();
        java.util.Set<Long> allowedIds = new java.util.HashSet<>();

        // Collect custom department IDs
        if (org.babyfish.jimmer.ImmutableObjects.isLoaded(user, UserProps.ROLES)) {
            for (var role : user.roles()) {
                if (role.dataScope() == DataScope.DEPT_CUSTOM) {
                    if (org.babyfish.jimmer.ImmutableObjects.isLoaded(role,
                            com.landfun.boot.modules.system.role.RoleProps.DEPTS) && role.depts() != null) {
                        role.depts().forEach(d -> allowedIds.add(d.id()));
                    }
                }
            }
        }

        if (hasRecursive && userDeptId != null) {
            allowedIds.addAll(getSubDeptIds(userDeptId));
        }

        if (hasSame && userDeptId != null) {
            allowedIds.add(userDeptId);
        }

        if (allowedIds.isEmpty()) {
            if (userDeptId != null) {
                // Default: see own dept
                args.where(args.getTable().id().eq(userDeptId));
            } else {
                // See nothing
                args.where(args.getTable().id().eq(-1L));
            }
        } else {
            args.where(args.getTable().id().in(allowedIds));
        }
    }

    private List<Long> getSubDeptIds(Long rootId) {
        // Fetch all depts (id, parent.id)
        List<Dept> allDepts = sqlClient
                .filters(cfg -> cfg.disable(this))
                .createQuery(DeptTable.$)
                .select(
                        DeptTable.$.fetch(
                                DeptFetcher.$
                                        .parent(DeptFetcher.$)))
                .execute();

        List<Long> result = new ArrayList<>();
        result.add(rootId);
        collectChildren(rootId, allDepts, result);
        return result;
    }

    private void collectChildren(Long parentId, List<Dept> allDepts, List<Long> result) {
        for (Dept dept : allDepts) {
            Dept parent = dept.parent();
            if (parent != null && parent.id() == parentId) {
                result.add(dept.id());
                collectChildren(dept.id(), allDepts, result);
            }
        }
    }
}
