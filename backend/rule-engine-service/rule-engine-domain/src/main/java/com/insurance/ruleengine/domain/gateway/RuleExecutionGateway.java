package com.insurance.ruleengine.domain.gateway;

import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleVersion;

public interface RuleExecutionGateway {
    void validateDrl(String drlContent);

    ExecutionResult execute(RuleVersion version, ExecutionRequest request);

    default void evict(String ruleCode, Integer version) {
        // no-op: overridden by caching implementations
    }
}

