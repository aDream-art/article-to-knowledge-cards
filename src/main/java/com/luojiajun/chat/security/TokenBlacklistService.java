package com.luojiajun.chat.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * JWT 黑名单：登出后把 jti 写进 Redis，TTL 等于 token 剩余有效期。
 * 这是"无状态 token 如何主动失效"的答案。
 */
@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "chat:jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
