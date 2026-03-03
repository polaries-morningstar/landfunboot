package com.landfun.boot.infrastructure.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.landfun.boot.infrastructure.service.DataScopeResolver.ScopeResult;
import com.landfun.boot.modules.system.dept.DeptFilter;
import com.landfun.boot.modules.system.dept.DeptTable;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.user.UserFilter;
import com.landfun.boot.modules.system.user.UserTable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides data-scope visibility: which user IDs the current user can "see"
 * (e.g. for message list by sender, or allowed recipient set when sending).
 * Returns null to mean "no restriction" (ALL / superuser).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private static final String CACHE_KEY_PREFIX = "dept:subtree:";
    private static final long CACHE_TTL_HOURS = 2;

    private final JSqlClient sqlClient;
    private final StringRedisTemplate redisTemplate;

    /**
     * Returns the set of user IDs the given user is allowed to see under data scope.
     * If the user has ALL scope or is superuser, returns null (no restriction).
     * Caller must treat null as "do not filter by user IDs".
     */
    public Set<Long> getVisibleUserIds(User currentUser) {
        ScopeResult scope = DataScopeResolver.resolve(currentUser, this::getSubDeptIds);

        if (scope.unrestricted()) {
            return null;
        }

        if (scope.selfOnly()) {
            return (currentUser != null) ? Set.of(currentUser.id()) : Set.of();
        }

        JSqlClient noFilter = sqlClient.filters(cfg ->
                cfg.disableByTypes(UserFilter.class, DeptFilter.class));

        List<Long> userIds = noFilter.createQuery(UserTable.$)
                .where(UserTable.$.deptId().in(scope.allowedDeptIds()))
                .select(UserTable.$.id())
                .execute();

        return new HashSet<>(userIds);
    }

    /**
     * Fetch all descendant dept IDs (including rootId) with Redis cache.
     */
    public List<Long> getSubDeptIds(Long rootId) {
        if (rootId == null) {
            return List.of();
        }

        String cacheKey = CACHE_KEY_PREFIX + rootId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return parseLongList(cached);
        }

        List<Long> result = computeSubDeptIds(rootId);

        try {
            redisTemplate.opsForValue().set(cacheKey, joinLongList(result), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache dept subtree for rootId {}: {}", rootId, e.getMessage());
        }

        return result;
    }

    /**
     * Evict cached dept subtree. Should be called when departments change.
     */
    public void evictSubDeptCache(Long deptId) {
        if (deptId != null) {
            redisTemplate.delete(CACHE_KEY_PREFIX + deptId);
        }
        Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private List<Long> computeSubDeptIds(Long rootId) {
        JSqlClient noFilter = sqlClient.filters(cfg ->
                cfg.disableByTypes(UserFilter.class, DeptFilter.class));

        List<Tuple2<Long, Long>> rows = noFilter.createQuery(DeptTable.$)
                .select(DeptTable.$.id(), DeptTable.$.parentId())
                .execute();

        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (Tuple2<Long, Long> row : rows) {
            Long id = row.get_1();
            Long parentId = row.get_2();
            if (parentId != null && parentId > 0) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(id);
            }
        }

        List<Long> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        collectChildren(rootId, childrenMap, result, visited);
        return result;
    }

    private void collectChildren(Long id, Map<Long, List<Long>> childrenMap, List<Long> result, Set<Long> visited) {
        if (!visited.add(id))
            return;
        result.add(id);
        List<Long> children = childrenMap.get(id);
        if (children != null) {
            for (Long childId : children) {
                collectChildren(childId, childrenMap, result, visited);
            }
        }
    }

    private static String joinLongList(List<Long> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private static List<Long> parseLongList(String s) {
        if (s == null || s.isBlank()) return List.of();
        String[] parts = s.split(",");
        List<Long> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(Long.parseLong(part.trim()));
        }
        return result;
    }
}
