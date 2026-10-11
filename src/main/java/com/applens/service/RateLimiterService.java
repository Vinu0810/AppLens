package com.applens.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Returns true if the request is allowed, false if rate limited.
     * Uses Redis atomic INCR + TTL for thread-safe counting.
     */
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);

            if (count == null) return true;

            // First request in this window — set the TTL
            if (count == 1) {
                redisTemplate.expire(key, window);
            }

            if (count > maxRequests) {
                log.warn("Rate limit exceeded for key: {}, count: {}", key, count);
                return false;
            }

            return true;

        } catch (Exception e) {
            // If Redis is down, fail open (allow the request)
            log.warn("Rate limiter Redis error, failing open: {}", e.getMessage());
            return true;
        }
    }

    public long getCount(String key) {
        try {
            Object val = redisTemplate.opsForValue().get(key);
            if (val == null) return 0;
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public long getTtlSeconds(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}