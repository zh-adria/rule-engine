package com.insurance.ruleengine.app;

import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.dto.ArchiveRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleSetCmd;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleSetCmd;
import com.insurance.ruleengine.client.dto.PublishRuleCmd;
import com.insurance.ruleengine.client.dto.ReviewApprovalCmd;
import com.insurance.ruleengine.client.dto.RollbackRuleCmd;
import com.insurance.ruleengine.client.dto.RuleAuditLogDTO;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionLogDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleSetDTO;
import com.insurance.ruleengine.client.dto.RuleSetExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleSetStepDTO;
import com.insurance.ruleengine.client.dto.RuleSetStepResultDTO;
import com.insurance.ruleengine.client.dto.RuleVersionDTO;
import com.insurance.ruleengine.client.dto.CustomFieldDTO;
import com.insurance.ruleengine.client.dto.SubmitApprovalCmd;
import com.insurance.ruleengine.domain.gateway.AuditGateway;
import com.insurance.ruleengine.domain.gateway.CryptoGateway;
import com.insurance.ruleengine.domain.gateway.CustomFieldGateway;
import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.gateway.RuleGateway;
import com.insurance.ruleengine.domain.gateway.RuleSetGateway;
import com.insurance.ruleengine.domain.model.DecisionType;
import com.insurance.ruleengine.domain.model.ExecutionMode;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleAuditLog;
import com.insurance.ruleengine.domain.model.RuleCategory;
import com.insurance.ruleengine.domain.model.RuleDefinition;
import com.insurance.ruleengine.domain.model.RuleExecutionLog;
import com.insurance.ruleengine.domain.model.RuleSet;
import com.insurance.ruleengine.domain.model.RuleSetStep;
import com.insurance.ruleengine.domain.model.RuleStatus;
import com.insurance.ruleengine.domain.model.RuleVersion;
import com.insurance.ruleengine.domain.model.IdempotencyRecord;
import com.insurance.ruleengine.domain.service.RulePolicy;
import com.insurance.ruleengine.domain.service.RuleSetExecutor;
import com.insurance.ruleengine.domain.gateway.ApprovalFlowGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.domain.gateway.WebhookGateway;
import com.insurance.ruleengine.domain.gateway.RuleTestGateway;
import com.insurance.ruleengine.domain.model.RuleTestCase;
import com.insurance.ruleengine.domain.model.RuleTestRun;
import com.insurance.ruleengine.domain.model.RuleTestSuite;
import com.insurance.ruleengine.domain.service.RuleTestAssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RuleEngineFacadeImpl implements RuleEngineFacade {
    private static final Logger log = LoggerFactory.getLogger(RuleEngineFacadeImpl.class);

    // ---- Phase 2: webhook event types ----
    public static final String EVENT_RULE_CREATED = "RULE_CREATED";
    public static final String EVENT_VERSION_CREATED = "VERSION_CREATED";
    public static final String EVENT_VERSION_SUBMITTED = "VERSION_SUBMITTED";
    public static final String EVENT_VERSION_APPROVED = "VERSION_APPROVED";
    public static final String EVENT_VERSION_REJECTED = "VERSION_REJECTED";
    public static final String EVENT_RULE_PUBLISHED = "RULE_PUBLISHED";
    public static final String EVENT_VERSION_ROLLED_BACK = "VERSION_ROLLED_BACK";
    public static final String EVENT_RULE_ARCHIVED = "RULE_ARCHIVED";

    private final RuleGateway ruleGateway;
    private final RuleExecutionGateway executionGateway;
    private final AuditGateway auditGateway;
    private CryptoGateway cryptoGateway;
    private final RuleSetGateway ruleSetGateway;
    private final ApprovalFlowGateway approvalFlowGateway;
    private final WebhookGateway webhookGateway;
    private final CustomFieldGateway customFieldGateway;
    private final ObjectMapper objectMapper;
    private final RulePolicy rulePolicy = new RulePolicy();
    private final com.insurance.ruleengine.domain.gateway.IdempotencyGateway idempotencyGateway;
    private RuleTestGateway ruleTestGateway;
    private RuleTestAssertionService ruleTestAssertionService;

    public RuleEngineFacadeImpl(RuleGateway ruleGateway, RuleExecutionGateway executionGateway,
                                AuditGateway auditGateway, CryptoGateway cryptoGateway,
                                RuleSetGateway ruleSetGateway, ApprovalFlowGateway approvalFlowGateway) {
        this(ruleGateway, executionGateway, auditGateway, cryptoGateway, ruleSetGateway, approvalFlowGateway,
                null, new ObjectMapper(), null, null);
    }

    public RuleEngineFacadeImpl(RuleGateway ruleGateway, RuleExecutionGateway executionGateway,
                                AuditGateway auditGateway, CryptoGateway cryptoGateway,
                                RuleSetGateway ruleSetGateway, ApprovalFlowGateway approvalFlowGateway,
                                CustomFieldGateway customFieldGateway) {
        this(ruleGateway, executionGateway, auditGateway, cryptoGateway, ruleSetGateway, approvalFlowGateway,
                null, new ObjectMapper(), null, customFieldGateway);
    }

    RuleEngineFacadeImpl(RuleGateway ruleGateway, RuleExecutionGateway executionGateway,
                         AuditGateway auditGateway, CryptoGateway cryptoGateway,
                         RuleSetGateway ruleSetGateway, ApprovalFlowGateway approvalFlowGateway,
                         WebhookGateway webhookGateway, ObjectMapper objectMapper,
                         com.insurance.ruleengine.domain.gateway.IdempotencyGateway idempotencyGateway,
                         CustomFieldGateway customFieldGateway) {
        this.ruleGateway = ruleGateway;
        this.executionGateway = executionGateway;
        this.auditGateway = auditGateway;
        this.cryptoGateway = cryptoGateway;
        this.ruleSetGateway = ruleSetGateway;
        this.approvalFlowGateway = approvalFlowGateway;
        this.webhookGateway = webhookGateway;
        this.customFieldGateway = customFieldGateway;
        this.objectMapper = objectMapper;
        this.idempotencyGateway = idempotencyGateway;
    }

    @Autowired(required = false)
    public void setRuleTestGate(RuleTestGateway ruleTestGateway, RuleTestAssertionService ruleTestAssertionService) {
        this.ruleTestGateway = ruleTestGateway;
        this.ruleTestAssertionService = ruleTestAssertionService;
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
        auditGateway.recordOperation(saved.getRuleCode(), null, "CREATE_RULE", cmd.getOwner(), "create rule", null,
                null, toJson(saved));
        fireWebhook(EVENT_RULE_CREATED, saved.getRuleCode(), null);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public RuleDTO createVersion(String ruleCode, CreateRuleVersionCmd cmd) {
        RuleDefinition rule = lockRule(ruleCode);
        ensureActive(rule);
        executionGateway.validateDrl(cmd.getDrlContent());
        String storedDrl = rule.isSensitive() ? cryptoGateway.encrypt(cmd.getDrlContent()) : cmd.getDrlContent();
        int versionNo = ruleGateway.nextVersion(ruleCode);
        RuleVersion version = RuleVersion.draft(ruleCode, versionNo, storedDrl, cmd.getVisualModel(),
                sha256(cmd.getDrlContent()), cmd.getCreatedBy());
        ruleGateway.saveVersion(version);
        auditGateway.recordOperation(ruleCode, versionNo, "CREATE_VERSION", cmd.getCreatedBy(), "draft version", null,
                null, toJson(Map.of("version", versionNo, "status", "DRAFT")));
        fireWebhook(EVENT_VERSION_CREATED, ruleCode, versionNo);
        RuleDTO dto = toDTO(rule, version);
        dto.setLatestVersion(versionNo);
        return dto;
    }

    @Override
    public RuleExecutionResultDTO testRule(String ruleCode, ExecuteRuleCmd cmd) {
        rulePolicy.validateFacts(cmd.getFacts());
        RuleDefinition rule = mustFindRule(ruleCode);
        ensureActive(rule);
        RuleVersion version = ruleGateway.findVersion(ruleCode, cmd.getVersion())
                .orElseThrow(() -> new IllegalArgumentException("version not found"));
        RuleVersion executable = decryptIfNeeded(rule, version);
        version.markTesting();
        ExecutionResult result = executionGateway.execute(executable, toRequest(cmd));
        fireWebhook(EVENT_VERSION_SUBMITTED, ruleCode, version.getVersion()); // treated as "version triggered"
        return toDTO(result);
    }

    @Override
    @Transactional
    public RuleDTO submitApproval(String ruleCode, Integer versionNo, SubmitApprovalCmd cmd) {
        RuleDefinition rule = mustFindRule(ruleCode);
        RuleVersion version = mustFindVersion(ruleCode, versionNo);
        // Guard: only one in-flight submission per rule
        if (version.getStatus() == RuleStatus.PENDING_APPROVAL) {
            return toDTO(rule, version);
        }
        version.submitApproval();
        RuleVersion saved = ruleGateway.saveVersion(version);
        auditGateway.recordOperation(ruleCode, versionNo, "SUBMIT_APPROVAL", cmd.getSubmittedBy(), cmd.getReason(), null,
                toJson(Map.of("status", "DRAFT/TESTING/REJECTED")), toJson(Map.of("status", "PENDING_APPROVAL")));
        fireWebhook(EVENT_VERSION_SUBMITTED, ruleCode, versionNo);

        // Call approval-flow-service. If it fails, throw to roll back the transaction
        // so the rule does not stay stuck in PENDING_APPROVAL.
        String targetId = ruleCode + ":" + versionNo;
        try {
            approvalFlowGateway.submitApproval("RULE_VERSION", targetId, cmd.getSubmittedBy(), cmd.getReason());
        } catch (RuntimeException e) {
            auditGateway.recordOperation(ruleCode, versionNo, "APPROVAL_FLOW_ERROR", cmd.getSubmittedBy(),
                    "Failed to create approval record: " + e.getMessage(), null, null, null);
            throw new IllegalStateException("approval-flow-service unavailable, please retry", e);
        }

        return toDTO(rule, saved);
    }

    @Override
    @Transactional
    public RuleDTO approveApproval(String ruleCode, Integer versionNo, ReviewApprovalCmd cmd) {
        RuleDefinition rule = mustFindRule(ruleCode);
        RuleVersion version = mustFindVersion(ruleCode, versionNo);
        version.markApproved(cmd.getReviewedBy());
        RuleVersion saved = ruleGateway.saveVersion(version);
        auditGateway.recordOperation(ruleCode, versionNo, "APPROVE", cmd.getReviewedBy(), cmd.getReason(), null,
                toJson(Map.of("status", "PENDING_APPROVAL")), toJson(Map.of("status", "APPROVED")));
        fireWebhook(EVENT_VERSION_APPROVED, ruleCode, versionNo);
        return toDTO(rule, saved);
    }

    @Override
    @Transactional
    public RuleDTO rejectApproval(String ruleCode, Integer versionNo, ReviewApprovalCmd cmd) {
        RuleDefinition rule = mustFindRule(ruleCode);
        RuleVersion version = mustFindVersion(ruleCode, versionNo);
        version.markRejected();
        RuleVersion saved = ruleGateway.saveVersion(version);
        auditGateway.recordOperation(ruleCode, versionNo, "REJECT", cmd.getReviewedBy(), cmd.getReason(), null,
                toJson(Map.of("status", "PENDING_APPROVAL")), toJson(Map.of("status", "REJECTED")));
        fireWebhook(EVENT_VERSION_REJECTED, ruleCode, versionNo);
        return toDTO(rule, saved);
    }

    @Override
    @Transactional
    public RuleDTO publish(String ruleCode, PublishRuleCmd cmd) {
        // P3-external: idempotency check
        if (cmd.getIdempotencyKey() != null && idempotencyGateway != null) {
            var existing = idempotencyGateway.findByKey(cmd.getIdempotencyKey());
            if (existing.isPresent() && !existing.get().isExpired()) {
                log.info("Idempotent publish hit: key={}", cmd.getIdempotencyKey());
                try {
                    return objectMapper.readValue(existing.get().getResponseBody(), RuleDTO.class);
                } catch (Exception e) {
                    log.warn("Failed to parse cached idempotency response, falling through", e);
                }
            }
        }
        RuleDefinition rule = mustFindRule(ruleCode);
        ensureActive(rule);
        RuleVersion version = mustFindVersion(ruleCode, cmd.getVersion());
        executionGateway.validateDrl(rule.isSensitive() ? cryptoGateway.decrypt(version.getDrlContent()) : version.getDrlContent());
        enforcePublishTestGate(rule, version, cmd);
        rule.publish(cmd.getVersion(), cmd.getGrayPercent());
        version.setEffectiveFrom(cmd.getEffectiveFrom());
        version.setEffectiveTo(cmd.getEffectiveTo());
        version.publish(cmd.getApprovedBy(), cmd.getGrayPercent() > 0 && cmd.getGrayPercent() < 100);
        ruleGateway.saveVersion(version);
        RuleDefinition saved = ruleGateway.saveRule(rule);
        auditGateway.recordOperation(ruleCode, cmd.getVersion(), "PUBLISH", cmd.getApprovedBy(), "publish version", null,
                toJson(snapshot("status", "APPROVED", "grayVersion", rule.getGrayVersion())),
                toJson(snapshot("status", cmd.getGrayPercent() > 0 && cmd.getGrayPercent() < 100 ? "GRAY" : "PUBLISHED",
                        "grayVersion", rule.getGrayVersion())));
        fireWebhook(EVENT_RULE_PUBLISHED, ruleCode, cmd.getVersion());
        executionGateway.evict(ruleCode, cmd.getVersion());
        // P3-external: save idempotency record
        if (cmd.getIdempotencyKey() != null && idempotencyGateway != null) {
            try {
                idempotencyGateway.save(IdempotencyRecord.create(
                        cmd.getIdempotencyKey(), "RULE_PUBLISH", ruleCode + ":" + cmd.getVersion(),
                        toJson(saved), java.time.Duration.ofHours(24)));
            } catch (Exception e) {
                log.warn("Failed to save idempotency record: {}", e.getMessage());
            }
        }
        return toDTO(saved);
    }

    @Override
    @Transactional
    public RuleDTO rollback(String ruleCode, RollbackRuleCmd cmd) {
        RuleDefinition rule = mustFindRule(ruleCode);
        ensureActive(rule);
        ruleGateway.findVersion(ruleCode, cmd.getTargetVersion())
                .orElseThrow(() -> new IllegalArgumentException("target version not found"));
        String beforeJson = toJson(snapshot("currentVersion", rule.getCurrentVersion(), "grayVersion", rule.getGrayVersion()));
        rule.rollback(cmd.getTargetVersion());
        RuleDefinition saved = ruleGateway.saveRule(rule);
        auditGateway.recordOperation(ruleCode, cmd.getTargetVersion(), "ROLLBACK", cmd.getOperator(), cmd.getReason(), null,
                beforeJson, toJson(snapshot("currentVersion", saved.getCurrentVersion(), "grayVersion", saved.getGrayVersion())));
        fireWebhook(EVENT_VERSION_ROLLED_BACK, ruleCode, cmd.getTargetVersion());
        executionGateway.evict(ruleCode, cmd.getTargetVersion());
        return toDTO(saved);
    }

    @Override
    public RuleExecutionResultDTO execute(ExecuteRuleCmd cmd) {
        rulePolicy.validateFacts(cmd.getFacts());
        RuleDefinition rule = mustFindRule(cmd.getRuleCode());
        ensureActive(rule);
        RuleVersion version = selectVersion(rule, cmd.getVersion(), cmd.getTraceId());
        RuleVersion executable = decryptIfNeeded(rule, version);
        ExecutionRequest request = toRequest(cmd);
        log.info("execute ruleCode={} version={} traceId={} scenario={}",
                cmd.getRuleCode(), version.getVersion(), request.getTraceId(), request.getScenario());
        long start = System.currentTimeMillis();
        ExecutionResult result = executionGateway.execute(executable, request);
        auditGateway.recordExecution(request, result);
        log.info("executed ruleCode={} traceId={} decision={} elapsed={}ms",
                cmd.getRuleCode(), request.getTraceId(), result.getDecision(),
                (System.currentTimeMillis() - start));
        return toDTO(result);
    }

    @Override
    public List<RuleDTO> listRules(String category, String businessLine, String status, String keyword) {
        return ruleGateway.listRules(category, businessLine, status, keyword)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RuleDTO getRule(String ruleCode) {
        RuleDefinition rule = mustFindRule(ruleCode);
        return toDTO(rule);
    }

    @Override
    public List<RuleVersionDTO> listVersions(String ruleCode) {
        mustFindRule(ruleCode);
        return ruleGateway.listVersions(ruleCode)
                .stream()
                .map(this::toVersionDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RuleVersionDTO getVersion(String ruleCode, Integer version) {
        mustFindRule(ruleCode);
        RuleVersion v = mustFindVersion(ruleCode, version);
        RuleVersionDTO dto = toVersionDTO(v);
        RuleDefinition rule = mustFindRule(ruleCode);
        RuleVersion decrypted = decryptIfNeeded(rule, v);
        dto.setDrlContent(decrypted.getDrlContent());
        return dto;
    }

    @Override
    public List<RuleExecutionLogDTO> listExecutions(String ruleCode) {
        mustFindRule(ruleCode);
        return auditGateway.listExecutions(ruleCode)
                .stream()
                .map(this::toExecutionLogDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RuleAuditLogDTO> listAudits(String ruleCode) {
        mustFindRule(ruleCode);
        return auditGateway.listAudits(ruleCode)
                .stream()
                .map(this::toAuditLogDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RuleDTO archive(String ruleCode, ArchiveRuleCmd cmd) {
        RuleDefinition rule = mustFindRule(ruleCode);
        if (rule.isArchived()) {
            throw new IllegalStateException("rule is already archived: " + ruleCode);
        }
        rule.archive();
        RuleDefinition saved = ruleGateway.saveRule(rule);
        auditGateway.recordOperation(ruleCode, null, "ARCHIVE", cmd.getOperator(), cmd.getReason(), null,
                toJson(Map.of("archived", false)), toJson(Map.of("archived", true)));
        fireWebhook(EVENT_RULE_ARCHIVED, ruleCode, null);
        return toDTO(saved);
    }

    // ---- Approval callback (called by approval-flow-service) ----

    @Override
    @Transactional
    public RuleDTO handleApprovalCallback(String ruleCode, Integer versionNo, String status, String reviewedBy, String reason) {
        RuleDefinition rule = mustFindRule(ruleCode);
        RuleVersion version = mustFindVersion(ruleCode, versionNo);
        // Idempotency: only PENDING_APPROVAL accepts a callback. Repeated callbacks are no-ops.
        if (version.getStatus() != RuleStatus.PENDING_APPROVAL) {
            return toDTO(rule, version);
        }
        if ("APPROVED".equals(status)) {
            version.markApproved(reviewedBy);
            auditGateway.recordOperation(ruleCode, versionNo, "APPROVAL_CALLBACK_APPROVED", reviewedBy, reason, null);
        } else if ("REJECTED".equals(status)) {
            version.markRejected();
            auditGateway.recordOperation(ruleCode, versionNo, "APPROVAL_CALLBACK_REJECTED", reviewedBy, reason, null);
        } else {
            throw new IllegalArgumentException("unknown approval status: " + status);
        }
        RuleVersion saved = ruleGateway.saveVersion(version);
        return toDTO(rule, saved);
    }

    // ---- Rule Set operations ----

    @Override
    @Transactional
    public RuleSetDTO createRuleSet(CreateRuleSetCmd cmd) {
        if (ruleSetGateway.existsRuleSet(cmd.getSetCode())) {
            throw new IllegalArgumentException("rule set already exists: " + cmd.getSetCode());
        }
        RuleSet ruleSet = RuleSet.create(cmd.getSetCode(), cmd.getSetName(), cmd.getDescription(), cmd.getOwner());
        for (RuleSetStepDTO stepDto : cmd.getSteps()) {
            RuleSetStep step = RuleSetStep.create(cmd.getSetCode(), stepDto.getStepOrder(),
                    stepDto.getRuleCode(), stepDto.getRuleVersion(),
                    ExecutionMode.valueOf(stepDto.getMode()), stepDto.isStopOnDecline());
            ruleSet.addStep(step);
        }
        RuleSet saved = ruleSetGateway.saveRuleSet(ruleSet);
        return toRuleSetDTO(saved);
    }

    @Override
    public RuleSetDTO getRuleSet(String setCode) {
        RuleSet ruleSet = ruleSetGateway.findRuleSet(setCode)
                .orElseThrow(() -> new IllegalArgumentException("rule set not found: " + setCode));
        return toRuleSetDTO(ruleSet);
    }

    @Override
    public List<RuleSetDTO> listRuleSets() {
        return ruleSetGateway.findAllRuleSets().stream()
                .map(this::toRuleSetDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RuleSetDTO updateRuleSet(String setCode, CreateRuleSetCmd cmd) {
        RuleSet existing = ruleSetGateway.findRuleSet(setCode)
                .orElseThrow(() -> new IllegalArgumentException("rule set not found: " + setCode));
        existing.setSetName(cmd.getSetName());
        existing.setDescription(cmd.getDescription());
        existing.setOwner(cmd.getOwner());
        existing.getSteps().clear();
        for (RuleSetStepDTO stepDto : cmd.getSteps()) {
            RuleSetStep step = RuleSetStep.create(setCode, stepDto.getStepOrder(),
                    stepDto.getRuleCode(), stepDto.getRuleVersion(),
                    ExecutionMode.valueOf(stepDto.getMode()), stepDto.isStopOnDecline());
            existing.addStep(step);
        }
        RuleSet saved = ruleSetGateway.saveRuleSet(existing);
        return toRuleSetDTO(saved);
    }

    @Override
    @Transactional
    public void deleteRuleSet(String setCode) {
        ruleSetGateway.deleteRuleSet(setCode);
    }

    @Override
    public RuleSetExecutionResultDTO executeRuleSet(ExecuteRuleSetCmd cmd) {
        rulePolicy.validateFacts(cmd.getFacts());
        RuleSet ruleSet = ruleSetGateway.findRuleSet(cmd.getSetCode())
                .orElseThrow(() -> new IllegalArgumentException("rule set not found: " + cmd.getSetCode()));

        long startNanos = System.nanoTime();

        RuleSetExecutor executor = new RuleSetExecutor(ruleGateway, executionGateway, this::decryptIfNeeded);
        RuleSetExecutor.RuleSetOutput out = executor.execute(ruleSet, cmd.getFacts(),
                cmd.getScenario(), cmd.getOperator(), cmd.getTraceId());

        RuleSetExecutionResultDTO result = new RuleSetExecutionResultDTO();
        result.setTraceId(cmd.getTraceId() != null ? cmd.getTraceId() : UUID.randomUUID().toString());
        result.setSetCode(cmd.getSetCode());
        result.setDecision(out.finalDecision.name());
        result.setHitRules(out.allHitRules);
        result.setOutputs(out.mergedOutputs);
        result.setStepResults(out.stepOutputs.stream().map(so -> {
            RuleSetStepResultDTO dto = new RuleSetStepResultDTO();
            dto.setStepOrder(so.stepOrder);
            dto.setRuleCode(so.ruleCode);
            dto.setDecision(so.decision != null ? so.decision.name() : (so.error ? "ERROR" : "ACCEPT"));
            dto.setSkipped(so.skipped);
            return dto;
        }).collect(Collectors.toList()));
        result.setElapsedMs((System.nanoTime() - startNanos) / 1_000_000);
        return result;
    }

    // ---- Custom Fields ----

    @Override
    public List<CustomFieldDTO> listCustomFields(String businessLine) {
        return customFieldGateway.findByBusinessLine(businessLine).stream()
                .map(this::toCustomFieldDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomFieldDTO> listAllCustomFields() {
        return customFieldGateway.findAll().stream()
                .map(this::toCustomFieldDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomFieldDTO createCustomField(CustomFieldDTO dto) {
        com.insurance.ruleengine.domain.model.CustomField field = com.insurance.ruleengine.domain.model.CustomField.create(
                dto.getFieldCode(), dto.getFieldLabel(), dto.getFieldType(),
                dto.getBusinessLine(), dto.getSortOrder());
        com.insurance.ruleengine.domain.model.CustomField saved = customFieldGateway.save(field);
        return toCustomFieldDTO(saved);
    }

    @Override
    public void deleteCustomField(Long id) {
        customFieldGateway.delete(id);
    }

    private CustomFieldDTO toCustomFieldDTO(com.insurance.ruleengine.domain.model.CustomField f) {
        CustomFieldDTO dto = new CustomFieldDTO();
        dto.setId(f.getId());
        dto.setFieldCode(f.getFieldCode());
        dto.setFieldLabel(f.getFieldLabel());
        dto.setFieldType(f.getFieldType());
        dto.setBusinessLine(f.getBusinessLine());
        dto.setSortOrder(f.getSortOrder());
        dto.setEnabled(f.getEnabled());
        return dto;
    }

    private RuleSetDTO toRuleSetDTO(RuleSet ruleSet) {
        RuleSetDTO dto = new RuleSetDTO();
        dto.setSetCode(ruleSet.getSetCode());
        dto.setSetName(ruleSet.getSetName());
        dto.setDescription(ruleSet.getDescription());
        dto.setOwner(ruleSet.getOwner());
        if (ruleSet.getSteps() != null) {
            dto.setSteps(ruleSet.getSteps().stream().map(step -> {
                RuleSetStepDTO stepDto = new RuleSetStepDTO();
                stepDto.setStepOrder(step.getStepOrder());
                stepDto.setRuleCode(step.getRuleCode());
                stepDto.setRuleVersion(step.getRuleVersion());
                stepDto.setMode(step.getMode().name());
                stepDto.setStopOnDecline(step.isStopOnDecline());
                return stepDto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }

    private void ensureActive(RuleDefinition rule) {
        if (rule.isArchived()) {
            throw new IllegalStateException("rule is archived: " + rule.getRuleCode());
        }
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

    private RuleVersion mustFindVersion(String ruleCode, Integer version) {
        return ruleGateway.findVersion(ruleCode, version)
                .orElseThrow(() -> new IllegalArgumentException("version not found"));
    }

    private void enforcePublishTestGate(RuleDefinition rule, RuleVersion version, PublishRuleCmd cmd) {
        if (ruleTestGateway == null || ruleTestAssertionService == null) {
            return;
        }
        List<RuleTestSuite> suites = ruleTestGateway.listSuites(rule.getRuleCode(), rule.getBusinessLine(), true);
        if (suites.isEmpty()) {
            return;
        }

        RuleVersion executable = decryptIfNeeded(rule, version);
        for (RuleTestSuite suite : suites) {
            List<RuleTestCase> cases = ruleTestGateway.listSuiteCases(suite.getSuiteCode()).stream()
                    .filter(RuleTestCase::isEnabled)
                    .collect(Collectors.toList());
            if (cases.isEmpty()) {
                RuleTestRun emptyRun = RuleTestRun.started(rule.getRuleCode(), suite.getSuiteCode(), null, cmd.getApprovedBy());
                emptyRun.finish(0, 0, 1, toJson(Map.of(
                        "suiteCode", suite.getSuiteCode(),
                        "errors", List.of("test suite has no enabled cases"))));
                ruleTestGateway.saveRun(emptyRun);
                throw new IllegalStateException("test gate failed: suite " + suite.getSuiteCode() + " has no enabled cases");
            }

            RuleTestRun run = RuleTestRun.started(rule.getRuleCode(), suite.getSuiteCode(), null, cmd.getApprovedBy());
            List<GateCaseResult> results = new ArrayList<>();
            for (RuleTestCase testCase : cases) {
                results.add(executeGateCase(executable, testCase, cmd.getApprovedBy()));
            }
            int passed = (int) results.stream().filter(GateCaseResult::passed).count();
            int failed = results.size() - passed;
            run.finish(results.size(), passed, failed, toJson(Map.of("cases", results)));
            ruleTestGateway.saveRun(run);
            if (failed > 0) {
                throw new IllegalStateException("test gate failed: suite " + suite.getSuiteCode() + " failed "
                        + failed + " of " + results.size() + " cases");
            }
        }
    }

    private GateCaseResult executeGateCase(RuleVersion version, RuleTestCase testCase, String executedBy) {
        try {
            ExecutionRequest request = new ExecutionRequest();
            request.setRuleCode(testCase.getRuleCode());
            request.setVersion(version.getVersion());
            request.setScenario(testCase.getScenario() == null ? "PUBLISH_GATE" : testCase.getScenario());
            request.setFacts(objectMapper.readValue(testCase.getFactsJson(), new TypeReference<>() {}));
            request.setOperator(executedBy);
            request.setTraceId(UUID.randomUUID().toString());
            ExecutionResult actual = executionGateway.execute(version, request);
            List<String> errors = ruleTestAssertionService.assertResult(testCase, actual);
            return new GateCaseResult(testCase.getCaseCode(), errors.isEmpty(), errors, actual);
        } catch (Exception e) {
            return new GateCaseResult(testCase.getCaseCode(), false, List.of(e.getMessage()), null);
        }
    }

    private RuleVersion selectVersion(RuleDefinition rule, Integer explicitVersion, String traceId) {
        RuleVersion version;
        if (explicitVersion != null) {
            version = ruleGateway.findVersion(rule.getRuleCode(), explicitVersion)
                    .orElseThrow(() -> new IllegalArgumentException("version not found"));
        } else if (rule.getGrayVersion() != null && rule.getGrayPercent() != null) {
            int bucket = Math.abs((traceId == null ? UUID.randomUUID().toString() : traceId).hashCode()) % 100;
            if (bucket < rule.getGrayPercent()) {
                version = ruleGateway.findVersion(rule.getRuleCode(), rule.getGrayVersion())
                        .orElseThrow(() -> new IllegalStateException("gray version missing"));
            } else {
                version = ruleGateway.findCurrentVersion(rule.getRuleCode())
                        .orElseThrow(() -> new IllegalStateException("no published version"));
            }
        } else {
            version = ruleGateway.findCurrentVersion(rule.getRuleCode())
                    .orElseThrow(() -> new IllegalStateException("no published version"));
        }
        // P2-2: time-window check — version is only active within effectiveFrom/effectiveTo
        LocalDateTime now = LocalDateTime.now();
        if (version.getEffectiveFrom() != null && now.isBefore(version.getEffectiveFrom())) {
            throw new IllegalStateException("version " + version.getVersion() + " not yet effective (from " + version.getEffectiveFrom() + ")");
        }
        if (version.getEffectiveTo() != null && now.isAfter(version.getEffectiveTo())) {
            throw new IllegalStateException("version " + version.getVersion() + " expired at " + version.getEffectiveTo());
        }
        return version;
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
        dto.setDescription(rule.getDescription());
        dto.setOwner(rule.getOwner());
        dto.setRegulatoryRef(rule.getRegulatoryRef());
        dto.setArchived(rule.isArchived());
        dto.setCurrentVersion(rule.getCurrentVersion());
        dto.setGrayVersion(rule.getGrayVersion());
        dto.setGrayPercent(rule.getGrayPercent());
        dto.setLatestVersion(rule.getGrayVersion() == null ? rule.getCurrentVersion() : rule.getGrayVersion());
        if (rule.isArchived()) {
            dto.setStatus("ARCHIVED");
        } else if (rule.getGrayVersion() != null) {
            dto.setStatus("GRAY");
        } else {
            dto.setStatus("PUBLISHED");
        }
        return dto;
    }

    private RuleDTO toDTO(RuleDefinition rule, RuleVersion version) {
        RuleDTO dto = toDTO(rule);
        dto.setLatestVersion(version.getVersion());
        dto.setStatus(version.getStatus().name());
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

    private RuleVersionDTO toVersionDTO(RuleVersion version) {
        RuleVersionDTO dto = new RuleVersionDTO();
        dto.setRuleCode(version.getRuleCode());
        dto.setVersion(version.getVersion());
        dto.setStatus(version.getStatus().name());
        dto.setChecksum(version.getChecksum());
        dto.setCreatedBy(version.getCreatedBy());
        dto.setApprovedBy(version.getApprovedBy());
        dto.setPublishedAt(version.getPublishedAt());
        dto.setEffectiveFrom(version.getEffectiveFrom());
        dto.setEffectiveTo(version.getEffectiveTo());
        return dto;
    }

    private RuleExecutionLogDTO toExecutionLogDTO(RuleExecutionLog log) {
        RuleExecutionLogDTO dto = new RuleExecutionLogDTO();
        dto.setTraceId(log.getTraceId());
        dto.setRuleCode(log.getRuleCode());
        dto.setVersion(log.getVersion());
        dto.setScenario(log.getScenario());
        dto.setDecision(log.getDecision());
        dto.setHitRules(log.getHitRules());
        dto.setElapsedMs(log.getElapsedMs());
        dto.setOperator(log.getOperator());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }

    private RuleAuditLogDTO toAuditLogDTO(RuleAuditLog log) {
        RuleAuditLogDTO dto = new RuleAuditLogDTO();
        dto.setRuleCode(log.getRuleCode());
        dto.setVersion(log.getVersion());
        dto.setAction(log.getAction());
        dto.setOperator(log.getOperator());
        dto.setReason(log.getReason());
        dto.setIpAddress(log.getIpAddress());
        dto.setCreatedAt(log.getCreatedAt());
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

    // ---- Phase 2: webhook + Phase 3: audit snapshot helpers ----

    @SafeVarargs
    private static Map<String, Object> snapshot(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("snapshot requires even number of arguments (key-value pairs), got " + entries.length);
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }

    private void fireWebhook(String eventType, String ruleCode, Integer version) {
        if (webhookGateway == null) {
            return;
        }
        String payload = toJson(Map.of(
                "event", eventType,
                "ruleCode", ruleCode,
                "version", version == null ? 0 : version,
                "timestamp", System.currentTimeMillis()));
        try {
            webhookGateway.sendAsync(eventType, payload);
        } catch (Exception e) {
            // webhook failure must not break the main transaction
            log.warn("webhook dispatch failed event={} ruleCode={}: {}", eventType, ruleCode, e.getMessage());
        }
    }

    private String toJson(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private record GateCaseResult(String caseCode, boolean passed, List<String> errors, ExecutionResult actual) {
    }
}
