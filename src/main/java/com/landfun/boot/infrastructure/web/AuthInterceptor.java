package com.landfun.boot.infrastructure.web;

import com.landfun.boot.infrastructure.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import org.babyfish.jimmer.sql.JSqlClient;
import com.landfun.boot.modules.system.user.UserTable;
import com.landfun.boot.modules.system.user.UserFetcher;
import com.landfun.boot.modules.system.role.RoleFetcher;
import com.landfun.boot.modules.system.dept.DeptFetcher;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final JSqlClient sqlClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        log.info("Checking Auth for URI: {}, Header: {}", request.getRequestURI(), authHeader);

        if (!StringUtils.hasText(authHeader)) {
            log.warn("Missing Authorization Header");
            response.setStatus(401);
            return false;
        }

        // Bearer token support
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        try {
            Claims claims = jwtUtils.parseToken(token);
            String userIdStr = claims.getSubject();
            long userId = Long.parseLong(userIdStr);

            // Double Check Redis for Kickout
            String redisKey = "user:token:" + userId;
            String redisToken = redisTemplate.opsForValue().get(redisKey);

            log.info("Claims UserId: {}, RedisToken exists: {}", userId, redisToken != null);

            if (redisToken == null || !redisToken.equals(token)) {
                log.warn("Token mismatch or expired in Redis for user {}", userId);
                response.setStatus(401);
                return false;
            }

            // Fetch User with Roles and Dept once per request
            com.landfun.boot.modules.system.user.User user = sqlClient.createQuery(UserTable.$)
                    .where(UserTable.$.id().eq(userId))
                    .select(
                            UserTable.$.fetch(
                                    UserFetcher.$
                                            .dept()
                                            .roles(
                                                    RoleFetcher.$
                                                            .allScalarFields()
                                                            .depts(DeptFetcher.$))))
                    .fetchOneOrNull();

            if (user == null) {
                log.warn("User {} not found in database", userId);
                response.setStatus(401);
                return false;
            }

            AuthContext.setUserId(userId);
            AuthContext.setUser(user);
            return true;
        } catch (Exception e) {
            log.warn("Auth failed: {}", e.getMessage(), e);
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        AuthContext.clear();
    }
}
