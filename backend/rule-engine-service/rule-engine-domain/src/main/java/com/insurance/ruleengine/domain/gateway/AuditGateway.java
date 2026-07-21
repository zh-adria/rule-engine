package com.insurance.ruleengine.domain.gateway;

import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleAuditLog;
import com.insurance.ruleengine.domain.model.RuleExecutionLog;

import java.util.List;

public interface AuditGateway {
    void recordOperation(String ruleCode, Integer version, String action, String operator, String reason, String ipAddress);

    /**
     * P1-3: record operation with before/after snapshot and chain hash.
     */
    void recordOperation(String ruleCode, Integer version, String action, String operator, String reason, String ipAddress,
                         String beforeJson, String afterJson);

    void recordExecution(ExecutionRequest request, ExecutionResult result);

    List<RuleExecutionLog> listExecutions(String ruleCode);

    List<RuleAuditLog> listAudits(String ruleCode);
}

