package com.insurance.ruleengine.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.insurance.ruleengine.domain.util.CryptoUtil;
import java.util.concurrent.TimeUnit;

/**
 * P2-1: Redis execution result cache decorator.
 * Wraps the Drools gateway — caches ExecutionResult keyed by ruleCode:version:factsHash.
 * Call evict() after rule version changes to invalidate stale entries.
 */
@Component
@Primary
public class RedisRuleExecutionCacheGateway implements RuleExecutionGateway {
    private static final Logger log = LoggerFactory.getLogger(RedisRuleExecutionCacheGateway.class);
    private static final String PREFIX = "rule:exec:";
    private static final long DEFAULT_TTL_SECONDS = 300;

    private final RuleExecutionGateway delegate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisRuleExecutionCacheGateway(@Qualifier("droolsRuleExecutionGateway") RuleExecutionGateway delegate,
                                          StringRedisTemplate redisTemplate,
                                          ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ExecutionResult execute(RuleVersion version, ExecutionRequest request) {
        String key = cacheKey(version, request);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Execution cache HIT: {}", key);
                return objectMapper.readValue(cached, ExecutionResult.class);
            }
        } catch (Exception e) {
            log.warn("Execution cache read failed, falling through: {}", e.getMessage());
        }

        ExecutionResult result = delegate.execute(version, request);
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Execution cache SET: {}", key);
        } catch (Exception e) {
            log.warn("Execution cache write failed: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public void validateDrl(String drlContent) {
        delegate.validateDrl(drlContent);
    }

    /**
     * Invalidate cached results for a specific rule version.
     */
    public void evict(String ruleCode, Integer version) {
        String pattern = PREFIX + ruleCode + ":" + version + ":*";
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} execution cache entries for {}:{}", keys.size(), ruleCode, version);
            }
        } catch (Exception e) {
            log.warn("Execution cache eviction failed: {}", e.getMessage());
        }
    }

    private String cacheKey(RuleVersion version, ExecutionRequest request) {
        String factsHash;
        try {
            String factsJson = objectMapper.writeValueAsString(request.getFacts());
            factsHash = sha256(factsJson);
        } catch (Exception e) {
            factsHash = String.valueOf(System.identityHashCode(request.getFacts()));
        }
        return PREFIX + version.getRuleCode() + ":" + version.getVersion() + ":" + factsHash;
    }

    private static String sha256(String input) {
        try {
            return CryptoUtil.sha256Hex(input);
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
