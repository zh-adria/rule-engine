package com.insurance.ruleengine.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.model.DecisionType;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleVersion;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRuleExecutionCacheGatewayTest {

    @Test
    void shouldCacheExecutionByRuleVersionAndFactsHash() throws Exception {
        AtomicInteger executions = new AtomicInteger();
        ObjectMapper objectMapper = new ObjectMapper();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null).thenAnswer(invocation -> {
            ExecutionResult cached = new ExecutionResult();
            cached.setRuleCode("RULE_CACHE");
            cached.setVersion(1);
            cached.setDecision(DecisionType.ACCEPT);
            cached.setTraceId("cached");
            return objectMapper.writeValueAsString(cached);
        });
        RuleExecutionGateway delegate = new RuleExecutionGateway() {
            @Override
            public void validateDrl(String drlContent) {
            }

            @Override
            public ExecutionResult execute(RuleVersion version, ExecutionRequest request) {
                executions.incrementAndGet();
                ExecutionResult result = new ExecutionResult();
                result.setRuleCode(version.getRuleCode());
                result.setVersion(version.getVersion());
                result.setDecision(DecisionType.ACCEPT);
                result.setTraceId(request.getTraceId());
                return result;
            }
        };
        RedisRuleExecutionCacheGateway gateway = new RedisRuleExecutionCacheGateway(delegate, redisTemplate, objectMapper);
        RuleVersion version = RuleVersion.draft("RULE_CACHE", 1, "rule", "{}", "checksum", "tester");
        ExecutionRequest request = new ExecutionRequest();
        request.setTraceId("trace-1");
        request.setFacts(Map.of("age", 40));

        gateway.execute(version, request);
        gateway.execute(version, request);

        assertEquals(1, executions.get());
        verify(valueOperations).set(anyString(), anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }
}
