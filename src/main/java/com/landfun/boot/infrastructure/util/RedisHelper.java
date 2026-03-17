package com.landfun.boot.infrastructure.util;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis utility methods that avoid O(N) KEYS commands in production.
 */
public final class RedisHelper {

    private RedisHelper() {}

    /**
     * Scan for keys matching the given pattern and delete them.
     * Uses SCAN instead of KEYS to avoid blocking Redis.
     */
    public static void scanAndDelete(StringRedisTemplate redisTemplate, String pattern) {
        Set<String> keysToDelete = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
            }
        }
        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }
    }

    /**
     * Scan for keys matching the given pattern and return them.
     * Uses SCAN instead of KEYS to avoid blocking Redis.
     */
    public static Set<String> scan(StringRedisTemplate redisTemplate, String pattern) {
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        }
        return result;
    }
}
