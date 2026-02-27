package com.landfun.boot.infrastructure.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.exception.BizException;
import com.landfun.boot.infrastructure.web.AuthContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(hasPermission)")
    public Object checkPermission(ProceedingJoinPoint point, HasPermission hasPermission) throws Throwable {
        com.landfun.boot.modules.system.user.User user = AuthContext.getUser();
        if (user == null) {
            throw new BizException(401, "Unauthorized");
        }

        // 1. Super Admin Bypass: if user is superuser, allow everything
        boolean isAdmin = user.isSuperuser();

        log.debug("Permission check - User: {}, Permission: {}, isSuperuser: {}",
                user.username(), hasPermission.value(), isAdmin);

        if (isAdmin) {
            log.debug("Super admin bypass triggered for user {}", user.username());
            return point.proceed();
        }

        // 2. Regular check via Redis Cache
        String permission = hasPermission.value();
        String redisKey = "user:permissions:" + user.id();

        Boolean hasIt = redisTemplate.opsForSet().isMember(redisKey, permission);
        // Also support '*' in redis for other potential super-roles stored in cache
        Boolean hasStar = redisTemplate.opsForSet().isMember(redisKey, "*");

        if (Boolean.TRUE.equals(hasIt) || Boolean.TRUE.equals(hasStar)) {
            return point.proceed();
        }

        throw new BizException(403, "Access Denied: Missing permission " + permission);
    }
}
