package com.luojiajun.chat.service;

import com.luojiajun.chat.common.ApiException;
import com.luojiajun.chat.common.ResultCode;
import com.luojiajun.chat.config.ChatProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 限流 + 配额：
 *  - 请求频次：Redis ZSET 滑动窗口 + Lua（单用户 20 次/分钟），解决固定窗口的临界双倍问题；
 *  - token 配额：Redis 日计数器，TTL 到次日零点；
 *  - 并发：单实例信号量，保护下游与线程池。
 */
@Slf4j
@Service
public class RateLimitService {

    private static final String SLIDING_WINDOW_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            local count = redis.call('ZCARD', key)
            if count >= limit then
                return 0
            end
            redis.call('ZADD', key, now, ARGV[4])
            redis.call('PEXPIRE', key, window)
            return 1
            """;

    private static final String QUOTA_SCRIPT = """
            local used = tonumber(redis.call('GET', KEYS[1]) or '0')
            local add = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            if used + add > limit then
                return -1
            end
            redis.call('INCRBY', KEYS[1], add)
            if redis.call('TTL', KEYS[1]) < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[3])
            end
            return used + add
            """;

    private static final DefaultRedisScript<Long> SLIDING_WINDOW =
            new DefaultRedisScript<>(SLIDING_WINDOW_SCRIPT, Long.class);
    private static final DefaultRedisScript<Long> QUOTA =
            new DefaultRedisScript<>(QUOTA_SCRIPT, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ChatProperties properties;
    private final Semaphore concurrency;

    public RateLimitService(StringRedisTemplate redisTemplate, ChatProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.concurrency = new Semaphore(Math.max(1, properties.getLimit().getMaxConcurrency()));
    }

    public void checkRequest(Long userId) {
        long now = System.currentTimeMillis();
        long windowMs = 60_000L;
        String key = "chat:rl:" + userId;
        Long allowed = redisTemplate.execute(SLIDING_WINDOW, List.of(key),
                String.valueOf(now), String.valueOf(windowMs),
                String.valueOf(properties.getLimit().getPerMinute()),
                now + ":" + UUID.randomUUID());
        if (allowed == null || allowed == 0L) {
            throw new ApiException(ResultCode.TOO_MANY_REQUESTS,
                    "请求过于频繁：每分钟最多 " + properties.getLimit().getPerMinute() + " 次");
        }
    }

    public void checkQuota(Long userId) {
        String value = redisTemplate.opsForValue().get(quotaKey(userId));
        int used = value == null ? 0 : Integer.parseInt(value);
        if (used >= properties.getLimit().getDailyTokens()) {
            throw new ApiException(ResultCode.QUOTA_EXCEEDED,
                    "今日 token 配额已用完（" + properties.getLimit().getDailyTokens() + "），明天再来");
        }
    }

    public void addTokens(Long userId, int tokens) {
        if (tokens <= 0) {
            return;
        }
        Long result = redisTemplate.execute(QUOTA, List.of(quotaKey(userId)),
                String.valueOf(tokens),
                String.valueOf(properties.getLimit().getDailyTokens()),
                String.valueOf(secondsToMidnight()));
        if (result != null && result < 0) {
            log.info("用户 {} 今日 token 配额已超限", userId);
        }
    }

    public int currentUsage(Long userId) {
        String value = redisTemplate.opsForValue().get(quotaKey(userId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    public boolean tryAcquireConcurrency(long timeoutSeconds) {
        try {
            return concurrency.tryAcquire(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void releaseConcurrency() {
        concurrency.release();
    }

    private String quotaKey(Long userId) {
        return "chat:quota:" + userId + ":" + LocalDate.now();
    }

    private long secondsToMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT);
        return Math.max(60, Duration.between(now, midnight).getSeconds());
    }
}
