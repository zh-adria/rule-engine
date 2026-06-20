package com.insurance.ruleengine.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.domain.gateway.AuditGateway;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleAuditLogEntity;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleExecutionLogEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleAuditLogJpaRepository;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleExecutionLogJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditGatewayImpl implements AuditGateway {
    private final RuleAuditLogJpaRepository auditRepository;
    private final RuleExecutionLogJpaRepository executionLogRepository;
    private final ObjectMapper objectMapper;

    public AuditGatewayImpl(RuleAuditLogJpaRepository auditRepository,
                            RuleExecutionLogJpaRepository executionLogRepository,
                            ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.executionLogRepository = executionLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void recordOperation(String ruleCode, Integer version, String action, String operator, String reason, String ipAddress) {
        RuleAuditLogEntity entity = new RuleAuditLogEntity();
        entity.setRuleCode(ruleCode);
        entity.setVersion(version);
        entity.setAction(action);
        entity.setOperator(operator);
        entity.setReason(reason);
        entity.setIpAddress(ipAddress);
        auditRepository.save(entity);
    }

    @Override
    public void recordExecution(ExecutionRequest request, ExecutionResult result) {
        RuleExecutionLogEntity entity = new RuleExecutionLogEntity();
        entity.setTraceId(result.getTraceId());
        entity.setRuleCode(result.getRuleCode());
        entity.setVersion(result.getVersion());
        entity.setScenario(request.getScenario());
        entity.setDecision(result.getDecision().name());
        entity.setHitRules(String.join(",", result.getHitRules()));
        entity.setElapsedMs(result.getElapsedMs());
        entity.setOperator(request.getOperator());
        entity.setRequestSnapshot(toJson(request.getFacts()));
        entity.setResponseSnapshot(toJson(result.getOutputs()));
        executionLogRepository.save(entity);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}

