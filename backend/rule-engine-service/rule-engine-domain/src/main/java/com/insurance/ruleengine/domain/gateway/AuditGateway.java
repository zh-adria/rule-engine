package com.insurance.ruleengine.domain.gateway;

import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;

public interface AuditGateway {
    void recordOperation(String ruleCode, Integer version, String action, String operator, String reason, String ipAddress);

    void recordExecution(ExecutionRequest request, ExecutionResult result);
}

