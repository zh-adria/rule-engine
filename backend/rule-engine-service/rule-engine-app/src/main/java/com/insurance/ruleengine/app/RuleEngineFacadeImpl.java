package com.insurance.ruleengine.app;

import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.dto.CreateRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.PublishRuleCmd;
import com.insurance.ruleengine.client.dto.RollbackRuleCmd;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import com.insurance.ruleengine.domain.gateway.AuditGateway;
import com.insurance.ruleengine.domain.gateway.CryptoGateway;
import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.gateway.RuleGateway;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleCategory;
import com.insurance.ruleengine.domain.model.RuleDefinition;
import com.insurance.ruleengine.domain.model.RuleVersion;
import com.insurance.ruleengine.domain.service.RulePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class RuleEngineFacadeImpl implements RuleEngineFacade {
    private final RuleGateway ruleGateway;
    private final RuleExecutionGateway executionGateway;
    private final AuditGateway auditGateway;
    private final CryptoGateway cryptoGateway;
    private final RulePolicy rulePolicy = new RulePolicy();

    public RuleEngineFacadeImpl(RuleGateway ruleGateway, RuleExecutionGateway executionGateway,
                                AuditGateway auditGateway, CryptoGateway cryptoGateway) {
        this.ruleGateway = ruleGateway;
        this.executionGateway = executionGateway;
        this.auditGateway = auditGateway;
        this.cryptoGateway = cryptoGateway;
    }

    @Override
    @Transactional
    public RuleDTO createRule(CreateRuleCmd cmd) {
        rulePolicy.validateRuleCode(cmd.getRuleCode());
        if (ruleGateway.existsRule(cmd.getRuleCode())) {
            throw new IllegalArgumentException("rule already exists: " + cmd.getRuleCode());
        }
        RuleDefinition rule = RuleDefinition.create(cmd.getRuleCode(), cmd.getRuleName(),
                RuleCategory.valueOf(cmd.getCategory()), cmd.getBusinessLine(), cmd.getDescription(),
                cmd.isSensitive(), cmd.getOwner(), cmd.getRegulatoryRef());
        RuleDefinition saved = ruleGateway.saveRule(rule);
        auditGateway.recordOperation(saved.getRuleCode(), null, "CREATE_RULE", cmd.getOwner(), "create rule", null);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public RuleDTO createVersion(String ruleCode, CreateRuleVersionCmd cmd) {
        RuleDefinition rule = lockRule(ruleCode);
        executionGateway.validateDrl(cmd.getDrlContent());
        String storedDrl = rule.isSensitive() ? cryptoGateway.encrypt(cmd.getDrlContent()) : cmd.getDrlContent();
        int versionNo = ruleGateway.nextVersion(ruleCode);
        RuleVersion version = RuleVersion.draft(ruleCode, versionNo, storedDrl, cmd.getVisualModel(),
                sha256(cmd.getDrlContent()), cmd.getCreatedBy());
        ruleGateway.saveVersion(version);
        auditGateway.recordOperation(ruleCode, versionNo, "CREATE_VERSION", cmd.getCreatedBy(), "draft version", null);
        RuleDTO dto = toDTO(rule);
        dto.setLatestVersion(versionNo);
        return dto;
    }

    @Override
    public RuleExecutionResultDTO testRule(String ruleCode, ExecuteRuleCmd cmd) {
        rulePolicy.validateFacts(cmd.getFacts());
        RuleDefinition rule = mustFindRule(ruleCode);
        RuleVersion version = ruleGateway.findVersion(ruleCode, cmd.getVersion())
                .orElseThrow(() -> new IllegalArgumentException("version not found"));
        RuleVersion executable = decryptIfNeeded(rule, version);
        version.markTesting();
        ExecutionResult result = executionGateway.execute(executable, toRequest(cmd));
        return toDTO(result);
    }

    @Override
    @Transactional
    public RuleDTO publish(String ruleCode, PublishRuleCmd cmd) {
        RuleDefinition rule = mustFindRule(ruleCode);
        RuleVersion version = ruleGateway.findVersion(ruleCode, cmd.getVersion())
                .orElseThrow(() -> new IllegalArgumentException("version not found"));
        executionGateway.validateDrl(rule.isSensitive() ? cryptoGateway.decrypt(version.getDrlContent()) : version.getDrlContent());
        rule.publish(cmd.getVersion(), cmd.getGrayPercent());
        version.publish(cmd.getApprovedBy(), cmd.getGrayPercent() > 0 && cmd.getGrayPercent() < 100);
        ruleGateway.saveVersion(version);
        RuleDefinition saved = ruleGateway.saveRule(rule);
        auditGateway.recordOperation(ruleCode, cmd.getVersion(), "PUBLISH", cmd.getApprovedBy(), "publish version", null);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public RuleDTO rollback(String ruleCode, RollbackRuleCmd cmd) {
        RuleDefinition rule = mustFindRule(ruleCode);
        ruleGateway.findVersion(ruleCode, cmd.getTargetVersion())
                .orElseThrow(() -> new IllegalArgumentException("target version not found"));
        rule.rollback(cmd.getTargetVersion());
        RuleDefinition saved = ruleGateway.saveRule(rule);
        auditGateway.recordOperation(ruleCode, cmd.getTargetVersion(), "ROLLBACK", cmd.getOperator(), cmd.getReason(), null);
        return toDTO(saved);
    }

    @Override
    public RuleExecutionResultDTO execute(ExecuteRuleCmd cmd) {
        rulePolicy.validateFacts(cmd.getFacts());
        RuleDefinition rule = mustFindRule(cmd.getRuleCode());
        RuleVersion version = selectVersion(rule, cmd.getVersion(), cmd.getTraceId());
        RuleVersion executable = decryptIfNeeded(rule, version);
        ExecutionRequest request = toRequest(cmd);
        ExecutionResult result = executionGateway.execute(executable, request);
        auditGateway.recordExecution(request, result);
        return toDTO(result);
    }

    private RuleDefinition mustFindRule(String ruleCode) {
        rulePolicy.validateRuleCode(ruleCode);
        return ruleGateway.findRule(ruleCode).orElseThrow(() -> new IllegalArgumentException("rule not found: " + ruleCode));
    }

    private RuleDefinition lockRule(String ruleCode) {
        rulePolicy.validateRuleCode(ruleCode);
        return ruleGateway.lockRuleForUpdate(ruleCode)
                .orElseThrow(() -> new IllegalArgumentException("rule not found: " + ruleCode));
    }

    private RuleVersion selectVersion(RuleDefinition rule, Integer explicitVersion, String traceId) {
        if (explicitVersion != null) {
            return ruleGateway.findVersion(rule.getRuleCode(), explicitVersion)
                    .orElseThrow(() -> new IllegalArgumentException("version not found"));
        }
        if (rule.getGrayVersion() != null && rule.getGrayPercent() != null) {
            int bucket = Math.abs((traceId == null ? UUID.randomUUID().toString() : traceId).hashCode()) % 100;
            if (bucket < rule.getGrayPercent()) {
                return ruleGateway.findVersion(rule.getRuleCode(), rule.getGrayVersion())
                        .orElseThrow(() -> new IllegalStateException("gray version missing"));
            }
        }
        return ruleGateway.findCurrentVersion(rule.getRuleCode())
                .orElseThrow(() -> new IllegalStateException("no published version"));
    }

    private RuleVersion decryptIfNeeded(RuleDefinition rule, RuleVersion version) {
        if (!rule.isSensitive()) {
            return version;
        }
        RuleVersion copy = RuleVersion.draft(version.getRuleCode(), version.getVersion(),
                cryptoGateway.decrypt(version.getDrlContent()), version.getVisualModel(), version.getChecksum(), version.getCreatedBy());
        copy.setStatus(version.getStatus());
        return copy;
    }

    private ExecutionRequest toRequest(ExecuteRuleCmd cmd) {
        ExecutionRequest request = new ExecutionRequest();
        request.setRuleCode(cmd.getRuleCode());
        request.setVersion(cmd.getVersion());
        request.setScenario(cmd.getScenario());
        request.setFacts(cmd.getFacts());
        request.setOperator(cmd.getOperator());
        request.setTraceId(cmd.getTraceId() == null || cmd.getTraceId().isBlank() ? UUID.randomUUID().toString() : cmd.getTraceId());
        return request;
    }

    private RuleDTO toDTO(RuleDefinition rule) {
        RuleDTO dto = new RuleDTO();
        dto.setRuleCode(rule.getRuleCode());
        dto.setRuleName(rule.getRuleName());
        dto.setCategory(rule.getCategory().name());
        dto.setBusinessLine(rule.getBusinessLine());
        dto.setCurrentVersion(rule.getCurrentVersion());
        dto.setGrayVersion(rule.getGrayVersion());
        dto.setGrayPercent(rule.getGrayPercent());
        dto.setLatestVersion(rule.getGrayVersion() == null ? rule.getCurrentVersion() : rule.getGrayVersion());
        dto.setStatus(rule.getGrayVersion() == null ? "PUBLISHED" : "GRAY");
        return dto;
    }

    private RuleExecutionResultDTO toDTO(ExecutionResult result) {
        RuleExecutionResultDTO dto = new RuleExecutionResultDTO();
        dto.setTraceId(result.getTraceId());
        dto.setRuleCode(result.getRuleCode());
        dto.setVersion(result.getVersion());
        dto.setDecision(result.getDecision().name());
        dto.setHitRules(result.getHitRules());
        dto.setOutputs(result.getOutputs());
        dto.setElapsedMs(result.getElapsedMs());
        return dto;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : encoded) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
