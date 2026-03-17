package com.landfun.boot.modules.auth.service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.landfun.boot.infrastructure.exception.BizException;
import com.landfun.boot.infrastructure.util.JwtUtils;
import com.landfun.boot.infrastructure.web.AuthContext;
import com.landfun.boot.modules.auth.dto.LoginReq;
import com.landfun.boot.modules.auth.dto.LoginResult;
import com.landfun.boot.modules.auth.dto.LoginResult.LoginUserInfo;
import com.landfun.boot.modules.auth.dto.UserInfoResult;
import com.landfun.boot.modules.system.dept.DeptFilter;
import com.landfun.boot.modules.system.menu.Menu;
import com.landfun.boot.modules.system.menu.MenuFetcher;
import com.landfun.boot.modules.system.menu.MenuService;
import com.landfun.boot.modules.system.menu.MenuTable;
import com.landfun.boot.modules.system.menu.MenuType;
import com.landfun.boot.modules.system.role.RoleFetcher;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.user.UserFetcher;
import com.landfun.boot.modules.system.user.UserFilter;
import com.landfun.boot.modules.system.user.UserTable;

import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JSqlClient sqlClient;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final MenuService menuService;

    public LoginResult login(LoginReq req) {
        if (req == null || req.email() == null || req.email().isBlank()) {
            throw new BizException(400, "请输入邮箱");
        }
        if (req.password() == null || req.password().isBlank()) {
            throw new BizException(400, "请输入密码");
        }
        log.debug("Login attempt for email: {}", req.email());
        UserTable t = UserTable.$;
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

        if (!BCrypt.checkpw(req.password(), user.password())) {
            log.warn("Password mismatch for user: {}", req.email());
            throw new BizException(401, "用户不存在或密码错误");
        }

        if (!user.isActive()) {
            throw new BizException(403, "账户已被禁用，请联系管理员");
        }

        String token = jwtUtils.createToken(user.id(), user.username());

        String tokenKey = "user:token:" + user.id();
        try {
            redisTemplate.opsForValue().set(tokenKey, token, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Redis set token failed for user {}: {}", user.id(), e.getMessage());
            throw new BizException(503, "服务暂时不可用，请稍后重试");
        }

        // Fetch user permissions (Role -> Menu -> Permission)
        User userWithRole = sqlClient
                .filters(cfg -> cfg.disableByTypes(UserFilter.class, DeptFilter.class))
                .createQuery(t)
                .where(t.id().eq(user.id()))
                .select(t.fetch(
                        UserFetcher.$
                                .allScalarFields()
                                .role(RoleFetcher.$
                                        .allScalarFields()
                                        .menus(MenuFetcher.$.allScalarFields()))))
                .fetchOne();

        Set<String> permissions = new HashSet<>();
        if (userWithRole != null && userWithRole.role() != null && userWithRole.role().menus() != null) {
            for (Menu menu : userWithRole.role().menus()) {
                if (menu != null && menu.permission() != null && !menu.permission().isEmpty()) {
                    permissions.add(menu.permission());
                }
            }
        }

        String permKey = "user:permissions:" + user.id();
        try {
            redisTemplate.delete(permKey);
            if (!permissions.isEmpty()) {
                redisTemplate.opsForSet().add(permKey, permissions.toArray(new String[0]));
                redisTemplate.expire(permKey, 30, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.warn("Redis set permissions failed for user {}: {}", user.id(), e.getMessage());
        }

        return new LoginResult(
                token,
                new LoginUserInfo(user.id(), user.username(), user.email(), user.isSuperuser()));
    }

    public UserInfoResult info() {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "Not authenticated");
        }

        User user = AuthContext.getUser();
        if (user == null) {
            user = sqlClient.createQuery(UserTable.$)
                    .where(UserTable.$.id().eq(userId))
                    .select(UserTable.$.fetch(UserFetcher.$.allScalarFields()))
                    .fetchOne();
        }

        if (user == null) {
            throw new BizException(401, "User not found");
        }

        Set<String> permissions;
        if (user.isSuperuser()) {
            // Superuser gets ALL permissions from ALL menus (not just their role)
            permissions = new HashSet<>();
            List<Menu> allMenus = sqlClient.createQuery(MenuTable.$)
                    .select(MenuTable.$.fetch(MenuFetcher.$.permission()))
                    .execute();
            for (Menu menu : allMenus) {
                if (menu.permission() != null && !menu.permission().isEmpty()) {
                    permissions.add(menu.permission());
                }
            }
        } else {
            String permKey = "user:permissions:" + userId;
            permissions = redisTemplate.opsForSet().members(permKey);
            if (permissions == null || permissions.isEmpty()) {
                permissions = new HashSet<>();
                User userWithRole = sqlClient.getEntities().findById(
                        UserFetcher.$.allScalarFields().role(
                                RoleFetcher.$.allScalarFields().menus(
                                        MenuFetcher.$.allScalarFields())),
                        userId);
                if (userWithRole != null && userWithRole.role() != null) {
                    for (Menu menu : userWithRole.role().menus()) {
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

        LoginUserInfo userInfo = new LoginUserInfo(user.id(), user.username(), user.email(), user.isSuperuser());
        return new UserInfoResult(userInfo, permissions);
    }

    /**
     * Returns menu tree for the current user (menus from user's role, DIR and MENU only).
     * Requires authentication; no sys:menu:list permission needed.
     */
    public List<Map<String, Object>> menus() {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "Not authenticated");
        }
        User userWithMenus = sqlClient
                .createQuery(UserTable.$)
                .where(UserTable.$.id().eq(userId))
                .select(UserTable.$.fetch(
                        UserFetcher.$.allScalarFields()
                                .role(RoleFetcher.$.allScalarFields()
                                        .menus(MenuFetcher.$.allScalarFields().parent(MenuFetcher.$.allScalarFields())))))
                .fetchOne();
        if (userWithMenus == null) {
            return List.of();
        }
        List<Menu> menusToShow;
        if (userWithMenus.isSuperuser()) {
            menusToShow = menuService.getAllMenusWithParent().stream()
                    .filter(m -> m.type() == MenuType.DIR || m.type() == MenuType.MENU)
                    .toList();
        } else if (userWithMenus.role() == null || userWithMenus.role().menus() == null) {
            return List.of();
        } else {
            menusToShow = userWithMenus.role().menus();
        }
        return menuService.treeFromMenus(menusToShow);
    }
}
