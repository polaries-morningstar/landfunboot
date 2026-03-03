package com.landfun.boot.modules.auth.service;

import java.time.Duration;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.landfun.boot.infrastructure.exception.BizException;
import com.landfun.boot.infrastructure.util.JwtUtils;
import com.landfun.boot.infrastructure.web.AuthContext;
import com.landfun.boot.modules.auth.dto.LoginReq;
import com.landfun.boot.modules.system.dept.DeptFilter;
import com.landfun.boot.modules.system.menu.Menu;
import com.landfun.boot.modules.system.menu.MenuFetcher;
import com.landfun.boot.modules.system.menu.MenuService;
import com.landfun.boot.modules.system.menu.MenuType;
import com.landfun.boot.modules.system.role.RoleFetcher;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.user.UserFetcher;
import com.landfun.boot.modules.system.user.UserFilter;
import com.landfun.boot.modules.system.user.UserTable;

import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JSqlClient sqlClient;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final MenuService menuService;

    public Object login(LoginReq req) {
        if (req == null || req.email() == null || req.email().isBlank()) {
            throw new BizException(400, "请输入邮箱");
        }
        if (req.password() == null || req.password().isBlank()) {
            throw new BizException(400, "请输入密码");
        }
        log.debug("Login attempt for email: {}", req.email());
        UserTable t = UserTable.$;
        // 登录时必须禁用过滤器，否则未登录状态下可能查不到用户
        User user = sqlClient
                .filters(cfg -> cfg.disableByTypes(UserFilter.class, DeptFilter.class))
                .createQuery(t)
                .where(t.email().eq(req.email().trim()))
                .select(t)
                .fetchOneOrNull();

        if (user == null) {
            log.warn("User not found: {}", req.email());
            throw new BizException(401, "用户不存在或密码错误");
        }
        log.debug("User found: {}, checking password...", user.username());

        if (!BCrypt.checkpw(req.password(), user.password())) {
            log.warn("Password mismatch for user: {}", req.email());
            throw new BizException(401, "用户不存在或密码错误");
        }
        log.debug("Password correct for user: {}", user.username());

        if (!user.isActive()) {
            throw new BizException(403, "账户已被禁用，请联系管理员");
        }

        log.debug("Generating token for user: {}", user.id());
        String token = jwtUtils.createToken(user.id(), user.username());

        // Store Token in Redis（失败时给出明确提示，便于排查重启后 Redis 未就绪等问题）
        String tokenKey = "user:token:" + user.id();
        try {
            redisTemplate.opsForValue().set(tokenKey, token, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Redis set token failed for user {}: {}", user.id(), e.getMessage());
            throw new BizException(503, "服务暂时不可用，请稍后重试");
        }

        // Fetch user permissions (Role -> Menu -> Permission)
        log.debug("Fetching role and menus for user: {}", user.id());
        User userWithRole = sqlClient
                .filters(cfg -> cfg.disableByTypes(UserFilter.class, DeptFilter.class))
                .createQuery(t)
                .where(t.id().eq(user.id()))
                .select(
                        t.fetch(
                                UserFetcher.$
                                        .allScalarFields()
                                        .role(
                                                RoleFetcher.$
                                                        .allScalarFields()
                                                        .menus(
                                                                MenuFetcher.$.allScalarFields()))))
                .fetchOne();
        log.debug("User with role fetched successfully.");

        // Collect permissions
        java.util.Set<String> permissions = new java.util.HashSet<>();
        if (userWithRole != null && userWithRole.role() != null && userWithRole.role().menus() != null) {
            for (com.landfun.boot.modules.system.menu.Menu menu : userWithRole.role().menus()) {
                if (menu != null && menu.permission() != null && !menu.permission().isEmpty()) {
                    permissions.add(menu.permission());
                }
            }
        }

        // Store Permissions in Redis (Set)
        String permKey = "user:permissions:" + user.id();
        try {
            redisTemplate.delete(permKey);
            if (!permissions.isEmpty()) {
                redisTemplate.opsForSet().add(permKey, permissions.toArray(new String[0]));
                redisTemplate.expire(permKey, 30, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.warn("Redis set permissions failed for user {}: {}", user.id(), e.getMessage());
            // 不阻断登录，仅记录；权限可后续从 DB 拉取
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.id());
        userData.put("username", user.username());
        userData.put("email", user.email());
        userData.put("isSuperuser", user.isSuperuser());
        result.put("user", userData);

        return result;
    }

    public Object info() {
        Long userId = com.landfun.boot.infrastructure.web.AuthContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "Not authenticated");
        }

        User user = com.landfun.boot.infrastructure.web.AuthContext.getUser();
        if (user == null) {
            // Fallback if context is somehow lost, though it shouldn't be
            user = sqlClient.createQuery(com.landfun.boot.modules.system.user.UserTable.$)
                    .where(com.landfun.boot.modules.system.user.UserTable.$.id().eq(userId))
                    .select(com.landfun.boot.modules.system.user.UserTable.$.fetch(
                            com.landfun.boot.modules.system.user.UserFetcher.$.allScalarFields()))
                    .fetchOne();
        }

        if (user == null) {
            throw new BizException(401, "User not found");
        }

        log.debug("User info requested for ID: {}, isSuperuser: {}", userId, user.isSuperuser());

        java.util.Set<String> permissions;
        if (user.isSuperuser()) {
            log.debug("Superuser detected, granting all permissions");
            // Fetch permissions from DB via bound role and its menus
            permissions = new java.util.HashSet<>();
            User userWithAll = sqlClient.getEntities().findById(
                    UserFetcher.$.allScalarFields().role(
                            RoleFetcher.$.allScalarFields().menus(
                                    MenuFetcher.$.allScalarFields())),
                    user.id());
            if (userWithAll != null && userWithAll.role() != null) {
                for (com.landfun.boot.modules.system.menu.Menu menu : userWithAll.role().menus()) {
                    if (menu != null && menu.permission() != null && !menu.permission().isEmpty()) {
                        permissions.add(menu.permission());
                    }
                }
            }
        } else {
            log.debug("Fetching permissions from Redis for user: {}", userId);
            String permKey = "user:permissions:" + userId;
            permissions = redisTemplate.opsForSet().members(permKey);
            if (permissions == null || permissions.isEmpty()) {
                log.debug("Redis cache miss, fetching from DB");
                permissions = new java.util.HashSet<>();
                User userWithRole = sqlClient.getEntities().findById(
                        UserFetcher.$.allScalarFields().role(
                                RoleFetcher.$.allScalarFields().menus(
                                        MenuFetcher.$.allScalarFields())),
                        userId);
                if (userWithRole != null && userWithRole.role() != null) {
                    for (com.landfun.boot.modules.system.menu.Menu menu : userWithRole.role().menus()) {
                        if (menu != null && menu.permission() != null && !menu.permission().isEmpty()) {
                            permissions.add(menu.permission());
                        }
                    }
                    if (!permissions.isEmpty()) {
                        redisTemplate.opsForSet().add(permKey, permissions.toArray(new String[0]));
                        redisTemplate.expire(permKey, Duration.ofDays(1));
                    }
                }
            }
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.id());
        userData.put("username", user.username());
        userData.put("email", user.email());
        userData.put("isSuperuser", user.isSuperuser());

        Map<String, Object> result = new HashMap<>();
        result.put("user", userData);
        result.put("permissions", permissions);

        return result;
    }

    /**
     * Returns menu tree for the current user (menus from user's role, DIR and MENU only).
     * Requires authentication; no sys:menu:list permission needed.
     */
    public Object menus() {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "Not authenticated");
        }
        User userWithMenus = sqlClient
                .createQuery(com.landfun.boot.modules.system.user.UserTable.$)
                .where(com.landfun.boot.modules.system.user.UserTable.$.id().eq(userId))
                .select(com.landfun.boot.modules.system.user.UserTable.$.fetch(
                        UserFetcher.$.allScalarFields()
                                .role(RoleFetcher.$.allScalarFields()
                                        .menus(MenuFetcher.$.allScalarFields().parent(MenuFetcher.$.allScalarFields())))))
                .fetchOne();
        if (userWithMenus == null) {
            return java.util.Collections.emptyList();
        }
        java.util.List<Menu> menusToShow;
        if (userWithMenus.isSuperuser()) {
            menusToShow = menuService.getAllMenusWithParent().stream()
                    .filter(m -> m.type() == MenuType.DIR || m.type() == MenuType.MENU)
                    .toList();
        } else if (userWithMenus.role() == null || userWithMenus.role().menus() == null) {
            return java.util.Collections.emptyList();
        } else {
            menusToShow = userWithMenus.role().menus();
        }
        return menuService.treeFromMenus(menusToShow);
    }
}
