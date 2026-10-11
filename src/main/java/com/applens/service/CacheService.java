package com.applens.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void set(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Cache SET: {}", key);
        } catch (Exception e) {
            log.warn("Cache SET failed for key {}: {}", key, e.getMessage());
        }
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw == null) return Optional.empty();
            T value = objectMapper.readValue(raw.toString(), type);
            log.debug("Cache HIT: {}", key);
            return Optional.of(value);
        } catch (Exception e) {
            log.warn("Cache GET failed for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Cache DEL: {}", key);
        } catch (Exception e) {
            log.warn("Cache DEL failed for key {}: {}", key, e.getMessage());
        }
    }

    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cache cleared {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Cache pattern delete failed for {}: {}", pattern, e.getMessage());
        }
    }

    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }
}