package com.landfun.boot.infrastructure.service;

import java.util.HashSet;
import java.util.Set;

import org.babyfish.jimmer.ImmutableObjects;

import com.landfun.boot.modules.system.role.DataScope;
import com.landfun.boot.modules.system.role.RoleProps;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.user.UserProps;

/**
 * Resolves the effective data scope for a given user into a {@link ScopeResult}.
 * Centralises the DataScope enum interpretation that was previously duplicated
 * across UserFilter, DeptFilter, and DataScopeService.
 */
public final class DataScopeResolver {

    private DataScopeResolver() {
    }

    public record ScopeResult(
            boolean unrestricted,
            boolean selfOnly,
            Set<Long> allowedDeptIds) {
    }

    private static final ScopeResult UNRESTRICTED = new ScopeResult(true, false, Set.of());
    private static final ScopeResult SELF_ONLY = new ScopeResult(false, true, Set.of());

    /**
     * @param user             the current authenticated user (may be null)
     * @param subDeptIdsFn     a function that returns all descendant dept IDs (including root)
     *                         for DEPT_RECURSIVE scope. Passed as a lambda to avoid a hard
     *                         dependency on DataScopeService (prevents circular init issues).
     * @return a {@link ScopeResult} describing the effective data scope
     */
    public static ScopeResult resolve(User user, java.util.function.Function<Long, java.util.List<Long>> subDeptIdsFn) {
        if (user == null) {
            return SELF_ONLY;
        }
        if (user.isSuperuser()) {
            return UNRESTRICTED;
        }

        DataScope scope = extractScope(user);

        if (scope == null || scope == DataScope.ALL) {
            return scope == DataScope.ALL ? UNRESTRICTED : SELF_ONLY;
        }

        if (scope == DataScope.SELF) {
            return SELF_ONLY;
        }

        Long userDeptId = (user.dept() != null) ? user.dept().id() : null;
        Set<Long> allowedDeptIds = new HashSet<>();

        switch (scope) {
            case DEPT_CUSTOM -> {
                var role = user.role();
                if (ImmutableObjects.isLoaded(role, RoleProps.DEPTS) && role.depts() != null) {
                    role.depts().forEach(d -> allowedDeptIds.add(d.id()));
                }
            }
            case DEPT_RECURSIVE -> {
                if (userDeptId != null) {
                    allowedDeptIds.addAll(subDeptIdsFn.apply(userDeptId));
                }
            }
            case DEPT_SAME -> {
                if (userDeptId != null) {
                    allowedDeptIds.add(userDeptId);
                }
            }
            default -> { }
        }

        if (allowedDeptIds.isEmpty()) {
            return SELF_ONLY;
        }
        return new ScopeResult(false, false, allowedDeptIds);
    }

    private static DataScope extractScope(User user) {
        if (ImmutableObjects.isLoaded(user, UserProps.ROLE) && user.role() != null) {
            return user.role().dataScope();
        }
        return null;
    }
}
