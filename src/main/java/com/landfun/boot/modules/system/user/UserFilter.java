package com.landfun.boot.modules.system.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.landfun.boot.infrastructure.web.AuthContext;
import com.landfun.boot.modules.system.role.DataScope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserFilter implements CacheableFilter<UserProps> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public SortedMap<String, Object> getParameters() {
        User user = AuthContext.getUser();
        if (user == null) {
            return null;
        }
        SortedMap<String, Object> map = new TreeMap<>();
        map.put("userId", user.id());
        return map;
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        if (e.getImmutableType().getJavaClass() == User.class) {
            return true;
        }
        return false;
    }

    @Override
    public void filter(FilterArgs<UserProps> args) {
        User user = AuthContext.getUser();
        if (user == null) {
            log.trace("No user in context, skipping UserFilter");
            return;
        }

        Long userId = user.id();
        boolean hasAll = false;
        boolean hasRecursive = false;
        boolean hasSame = false;
        boolean hasSelf = false;

        log.debug("Filtering User query for user: {} (id: {})", user.username(), userId);

        // Single-role model: inspect only user.role()
        if (org.babyfish.jimmer.ImmutableObjects.isLoaded(user, UserProps.ROLE) && user.role() != null) {
            var role = user.role();
            DataScope scope = role.dataScope();
            log.trace("User role: {}, dataScope: {}", role.name(), scope);
            if (scope == DataScope.ALL)
                hasAll = true;
            if (scope == DataScope.DEPT_RECURSIVE)
                hasRecursive = true;
            if (scope == DataScope.DEPT_SAME)
                hasSame = true;
            if (scope == DataScope.SELF)
                hasSelf = true;
        }

        if (hasAll || user.isSuperuser()) {
            log.debug("User has ALL scope or is superuser, skipping filter");
            return;
        }

        Long userDeptId = user.dept() == null ? null : user.dept().id();
        java.util.Set<Long> allowedIds = new java.util.HashSet<>();

        // Collect custom department IDs from single role (DEPT_CUSTOM)
        if (org.babyfish.jimmer.ImmutableObjects.isLoaded(user, UserProps.ROLE) && user.role() != null) {
            var role = user.role();
            if (role.dataScope() == DataScope.DEPT_CUSTOM) {
                if (org.babyfish.jimmer.ImmutableObjects.isLoaded(role,
                        com.landfun.boot.modules.system.role.RoleProps.DEPTS) && role.depts() != null) {
                    role.depts().forEach(d -> allowedIds.add(d.id()));
                }
            }
        }

        if (hasRecursive && userDeptId != null) {
            allowedIds.addAll(getSubDeptIds(userDeptId));
        }

        if (hasSame && userDeptId != null) {
            allowedIds.add(userDeptId);
        }

        log.debug("Filtering User query - hasRecursive: {}, hasSame: {}, hasSelf: {}, allowedIds: {}",
                hasRecursive, hasSame, hasSelf, allowedIds);

        if (allowedIds.isEmpty()) {
            if (hasSelf) {
                log.debug("Applying SELF filter - userId: {}", userId);
                args.where(args.getTable().id().eq(userId));
            } else if (userDeptId != null) {
                // Default: see own dept's users
                log.debug("Applying default DEPT_SAME filter - userDeptId: {}", userDeptId);
                args.where(args.getTable().dept().id().eq(userDeptId));
            } else {
                // See only self if no dept and no roles
                log.debug("No dept and no roles, applying default SELF filter - userId: {}", userId);
                args.where(args.getTable().id().eq(userId));
            }
        } else {
            log.debug("Applying DEPT filter for ids: {}", allowedIds);
            args.where(args.getTable().dept().id().in(allowedIds));
        }
    }

    /**
     * Fetch all descendant dept IDs (including rootId itself) using raw JDBC.
     * This deliberately avoids using Jimmer queries to prevent UserFilter
     * from being triggered recursively (which caused an infinite loop).
     */
    private List<Long> getSubDeptIds(Long rootId) {
        // Fetch all (id, parent_id) pairs from sys_dept table directly
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, parent_id FROM sys_dept");

        // Build a parent->children map
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = toLong(row.get("id"));
            Long parentId = toLong(row.get("parent_id"));
            if (parentId != null && parentId > 0) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(id);
            }
        }

        // BFS/DFS to collect all descendants
        List<Long> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        collectChildrenJdbc(rootId, childrenMap, result, visited);
        return result;
    }

    private void collectChildrenJdbc(Long id, Map<Long, List<Long>> childrenMap,
            List<Long> result, Set<Long> visited) {
        if (!visited.add(id))
            return;
        result.add(id);
        List<Long> children = childrenMap.get(id);
        if (children != null) {
            for (Long childId : children) {
                collectChildrenJdbc(childId, childrenMap, result, visited);
            }
        }
    }

    private Long toLong(Object val) {
        if (val == null)
            return null;
        if (val instanceof Long l)
            return l;
        if (val instanceof Number n)
            return n.longValue();
        return null;
    }
}
