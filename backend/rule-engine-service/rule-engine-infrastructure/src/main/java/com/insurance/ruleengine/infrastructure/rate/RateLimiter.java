package com.insurance.ruleengine.infrastructure.rate;

import com.insurance.ruleengine.infrastructure.config.RateLimitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Redis 的滑动窗口限流器
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitConfig config;

    // Lua 脚本：滑动窗口限流
    private static final String RATE_LIMIT_LUA_SCRIPT =
            "local key = KEYS[1]\n" +
            "local window = tonumber(ARGV[1])\n" +
            "local limit = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "local windowStart = now - window\n" +
            "\n" +
            "-- 移除窗口外的记录\n" +
            "redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)\n" +
            "\n" +
            "-- 获取当前窗口内的请求数\n" +
            "local count = redis.call('ZCARD', key)\n" +
            "\n" +
            "if count < limit then\n" +
            "    -- 允许请求，添加记录\n" +
            "    redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))\n" +
            "    redis.call('EXPIRE', key, window)\n" +
            "    return 1\n" +
            "else\n" +
            "    -- 超出限制\n" +
            "    return 0\n" +
            "end";

    private DefaultRedisScript<Long> redisScript;

    public RateLimiter(@Autowired(required = false) StringRedisTemplate redisTemplate, RateLimitConfig config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
        if (redisTemplate != null) {
            this.redisScript = new DefaultRedisScript<>();
            this.redisScript.setScriptText(RATE_LIMIT_LUA_SCRIPT);
            this.redisScript.setResultType(Long.class);
        }
    }

    /**
     * 检查请求是否允许
     *
     * @param key     限流键（如用户ID、IP地址）
     * @param limit   限制请求数
     * @return true 如果允许请求
     */
    public boolean isAllowed(String key, int limit) {
        if (!config.isEnabled() || redisTemplate == null) {
            return true;
        }

        try {
            String redisKey = "rate_limit:" + key;
            long now = Instant.now().getEpochSecond();

            Long result = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(redisKey),
                    String.valueOf(config.getWindowSeconds()),
                    String.valueOf(limit),
                    String.valueOf(now)
            );

            return result != null && result == 1;
        } catch (Exception e) {
            // Redis 不可用时放行
            log.warn("Rate limiter failed, allowing request: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 检查请求是否允许（使用默认限制）
     */
    public boolean isAllowed(String key) {
        return isAllowed(key, config.getDefaultLimit());
    }
}
