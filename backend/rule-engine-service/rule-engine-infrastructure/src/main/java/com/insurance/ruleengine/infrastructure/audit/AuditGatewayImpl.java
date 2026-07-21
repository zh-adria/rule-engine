package com.insurance.ruleengine.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.domain.gateway.AuditGateway;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleAuditLog;
import com.insurance.ruleengine.domain.model.RuleExecutionLog;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleAuditLogEntity;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleExecutionLogEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleAuditLogJpaRepository;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleExecutionLogJpaRepository;
import com.insurance.ruleengine.domain.util.CryptoUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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
        recordOperation(ruleCode, version, action, operator, reason, ipAddress, null, null);
    }

    @Override
    public void recordOperation(String ruleCode, Integer version, String action, String operator, String reason, String ipAddress,
                                String beforeJson, String afterJson) {
        // P1-3: chain hash — SHA-256(previous_hash + ruleCode + action + operator + afterJson)
        // content-based hash for deterministic verification; chain link via previous_hash
        String previousHash = auditRepository.findByRuleCodeOrderByIdDesc(ruleCode)
                .stream()
                .findFirst()
                .map(RuleAuditLogEntity::getAuditHash)
                .orElse("GENESIS");

        String auditHash = sha256(previousHash + "|" + safe(ruleCode) + "|" + safe(action) + "|"
                + safe(operator) + "|" + safe(afterJson));

        RuleAuditLogEntity entity = new RuleAuditLogEntity();
        entity.setRuleCode(ruleCode);
        entity.setVersion(version);
        entity.setAction(action);
        entity.setOperator(operator);
        entity.setReason(reason);
        entity.setIpAddress(ipAddress != null ? ipAddress : AuditContext.getIpAddress());
        entity.setBeforeJson(beforeJson);
        entity.setAfterJson(afterJson);
        entity.setPreviousHash(previousHash);
        entity.setAuditHash(auditHash);
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

    @Override
    public List<RuleExecutionLog> listExecutions(String ruleCode) {
        return executionLogRepository.findByRuleCodeOrderByCreatedAtDesc(ruleCode)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RuleAuditLog> listAudits(String ruleCode) {
        return auditRepository.findByRuleCodeOrderByCreatedAtDesc(ruleCode)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private RuleExecutionLog toDomain(RuleExecutionLogEntity entity) {
        RuleExecutionLog log = new RuleExecutionLog();
        log.setId(entity.getId());
        log.setTraceId(entity.getTraceId());
        log.setRuleCode(entity.getRuleCode());
        log.setVersion(entity.getVersion());
        log.setScenario(entity.getScenario());
        log.setDecision(entity.getDecision());
        log.setHitRules(entity.getHitRules());
        log.setElapsedMs(entity.getElapsedMs());
        log.setOperator(entity.getOperator());
        log.setCreatedAt(entity.getCreatedAt());
        return log;
    }

    private RuleAuditLog toDomain(RuleAuditLogEntity entity) {
        RuleAuditLog log = new RuleAuditLog();
        log.setId(entity.getId());
        log.setRuleCode(entity.getRuleCode());
        log.setVersion(entity.getVersion());
        log.setAction(entity.getAction());
        log.setOperator(entity.getOperator());
        log.setReason(entity.getReason());
        log.setIpAddress(entity.getIpAddress());
        log.setBeforeJson(entity.getBeforeJson());
        log.setAfterJson(entity.getAfterJson());
        log.setAuditHash(entity.getAuditHash());
        log.setPreviousHash(entity.getPreviousHash());
        log.setCreatedAt(entity.getCreatedAt());
        return log;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static String sha256(String input) {
        return CryptoUtil.sha256Hex(input);
    }
}

