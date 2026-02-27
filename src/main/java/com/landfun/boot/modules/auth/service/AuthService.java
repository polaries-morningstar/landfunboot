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
import com.landfun.boot.modules.auth.dto.LoginReq;
import com.landfun.boot.modules.system.menu.MenuFetcher;
import com.landfun.boot.modules.system.role.RoleFetcher;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.user.UserFetcher;
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

    public Object login(LoginReq req) {
        log.debug("Login attempt for email: {}", req.email());
        UserTable t = UserTable.$;
        User user = sqlClient.createQuery(t)
                .where(t.email().eq(req.email()))
                .select(t)
                .fetchOneOrNull();

        if (user == null) {
            log.warn("User not found: {}", req.email());
            throw new BizException("用户不存在: " + req.email());
        }
        log.debug("User found: {}, checking password...", user.username());

        if (!BCrypt.checkpw(req.password(), user.password())) {
            log.warn("Password mismatch for user: {}", req.email());
            throw new BizException("密码错误");
        }
        log.debug("Password correct for user: {}", user.username());

        if (!user.isActive()) {
            throw new BizException("账户已被禁用，请联系管理员");
        }

        log.debug("Generating token for user: {}", user.id());
        // Generate Token
        String token = jwtUtils.createToken(user.id(), user.username());

        // Store Token in Redis
        log.debug("Storing token in Redis for user: {}", user.id());
        String tokenKey = "user:token:" + user.id();
        redisTemplate.opsForValue().set(tokenKey, token, 30, TimeUnit.DAYS);

        // Fetch user permissions (Role -> Menu -> Permission)
        log.debug("Fetching role and menus for user: {}", user.id());
        User userWithRole = sqlClient
                .filters(cfg -> cfg.disableAll())
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
        if (userWithRole != null && userWithRole.role() != null) {
            for (com.landfun.boot.modules.system.menu.Menu menu : userWithRole.role().menus()) {
                if (menu != null && menu.permission() != null && !menu.permission().isEmpty()) {
                    permissions.add(menu.permission());
                }
            }
        }

        // Store Permissions in Redis (Set)
        log.debug("Storing permissions in Redis for user: {}", user.id());
        String permKey = "user:permissions:" + user.id();
        redisTemplate.delete(permKey); // Clear old
        if (!permissions.isEmpty()) {
            redisTemplate.opsForSet().add(permKey, permissions.toArray(new String[0]));
            redisTemplate.expire(permKey, 30, TimeUnit.DAYS);
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
}
