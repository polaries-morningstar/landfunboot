package com.landfun.boot.modules.auth.service;

import cn.hutool.crypto.digest.BCrypt;
import com.landfun.boot.infrastructure.exception.BizException;
import com.landfun.boot.infrastructure.util.JwtUtils;
import com.landfun.boot.modules.auth.dto.LoginReq;
import com.landfun.boot.modules.system.role.RoleFetcher;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.user.UserFetcher;
import com.landfun.boot.modules.system.user.UserTable;
import com.landfun.boot.modules.system.menu.MenuFetcher;
import lombok.RequiredArgsConstructor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JSqlClient sqlClient;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    public Object login(LoginReq req) {
        UserTable t = UserTable.$;
        User user = sqlClient.createQuery(t)
                .where(t.email().eq(req.email()))
                .select(t)
                .fetchOneOrNull();

        if (user == null) {
            throw new BizException("用户不存在: " + req.email());
        }

        if (!BCrypt.checkpw(req.password(), user.password())) {
            throw new BizException("密码错误");
        }

        if (!user.isActive()) {
            throw new BizException("账户已被禁用，请联系管理员");
        }

        // Generate Token
        String token = jwtUtils.createToken(user.id(), user.username());

        // Store Token in Redis
        String tokenKey = "user:token:" + user.id();
        redisTemplate.opsForValue().set(tokenKey, token, 30, TimeUnit.DAYS);

        // Fetch user permissions (Role -> Menu -> Permission)
        // Note: We need to load User with Roles and Menus.
        // For simplicity in this step, we will reload user with associations or use a
        // separate query.
        User userWithRoles = sqlClient.createQuery(t)
                .where(t.id().eq(user.id()))
                .select(
                        t.fetch(
                                UserFetcher.$.roles(
                                        RoleFetcher.$.menus(
                                                MenuFetcher.$.allScalarFields()))))
                .fetchOne();

        // Collect permissions
        java.util.Set<String> permissions = new java.util.HashSet<>();
        if (userWithRoles != null) {
            for (com.landfun.boot.modules.system.role.Role role : userWithRoles.roles()) {
                for (com.landfun.boot.modules.system.menu.Menu menu : role.menus()) {
                    if (menu.permission() != null && !menu.permission().isEmpty()) {
                        permissions.add(menu.permission());
                    }
                }
            }
        }

        // Store Permissions in Redis (Set)
        String permKey = "user:permissions:" + user.id();
        redisTemplate.delete(permKey); // Clear old
        if (!permissions.isEmpty()) {
            redisTemplate.opsForSet().add(permKey, permissions.toArray(new String[0]));
            redisTemplate.expire(permKey, 30, TimeUnit.DAYS);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }
}
