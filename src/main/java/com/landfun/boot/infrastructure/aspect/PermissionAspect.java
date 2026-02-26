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
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "Unauthorized");
        }

        String permission = hasPermission.value();
        String redisKey = "user:permissions:" + userId;

        Boolean hasIt = redisTemplate.opsForSet().isMember(redisKey, permission);
        Boolean isAdmin = redisTemplate.opsForSet().isMember(redisKey, "*");

        if (Boolean.TRUE.equals(hasIt) || Boolean.TRUE.equals(isAdmin)) {
            return point.proceed();
        }

        throw new BizException(403, "Access Denied: Missing permission " + permission);
    }
}
