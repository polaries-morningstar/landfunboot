package com.landfun.boot.infrastructure.web;

import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.landfun.boot.infrastructure.util.JwtUtils;
import com.landfun.boot.modules.system.dept.DeptFetcher;
import com.landfun.boot.modules.system.role.RoleFetcher;
import com.landfun.boot.modules.system.user.UserFetcher;
import com.landfun.boot.modules.system.user.UserTable;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        log.debug("Checking Auth for URI: {}, hasToken: {}", request.getRequestURI(), authHeader != null);

        // Bearer token support; fall back to ?token= query param (for SSE /
        // EventSource)
        String token;
        if (StringUtils.hasText(authHeader)) {
            token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        } else {
            token = request.getParameter("token");
        }

        if (!StringUtils.hasText(token)) {
            log.warn("Missing token in Authorization header and query param");
            response.setStatus(401);
            return false;
        }

        try {
            Claims claims = jwtUtils.parseToken(token);
            String userIdStr = claims.getSubject();
            long userId = Long.parseLong(userIdStr);

            // Double Check Redis for Kickout
            String redisKey = "user:token:" + userId;
            String redisToken = redisTemplate.opsForValue().get(redisKey);

            log.debug("Claims UserId: {}, RedisToken exists: {}", userId, redisToken != null);

            if (redisToken == null || !redisToken.equals(token)) {
                log.warn("Token mismatch or expired in Redis for user {}", userId);
                response.setStatus(401);
                return false;
            }

        // Fetch User with Role and Dept once per request
            com.landfun.boot.modules.system.user.User user = sqlClient.createQuery(UserTable.$)
                    .where(UserTable.$.id().eq(userId))
                    .select(
                            UserTable.$.fetch(
                                    UserFetcher.$
                                            .allScalarFields()
                                            .dept()
                                        .role(
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
        log.debug("User {} ({}) loaded with role: {}", user.username(), user.id(),
                user.role() != null ? user.role().code() : "NONE");
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
