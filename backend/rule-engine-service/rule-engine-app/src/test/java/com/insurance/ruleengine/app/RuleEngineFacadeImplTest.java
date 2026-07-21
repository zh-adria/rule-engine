package com.insurance.ruleengine.app;

import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.SubmitApprovalCmd;
import com.insurance.ruleengine.domain.gateway.ApprovalFlowGateway;
import com.insurance.ruleengine.domain.gateway.AuditGateway;
import com.insurance.ruleengine.domain.gateway.CustomFieldGateway;
import com.insurance.ruleengine.domain.gateway.CryptoGateway;
import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.gateway.RuleGateway;
import com.insurance.ruleengine.domain.gateway.RuleSetGateway;
import com.insurance.ruleengine.domain.gateway.RuleTestGateway;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleAuditLog;
import com.insurance.ruleengine.domain.model.RuleCategory;
import com.insurance.ruleengine.domain.model.RuleDefinition;
import com.insurance.ruleengine.domain.model.RuleExecutionLog;
import com.insurance.ruleengine.domain.model.RuleSet;
import com.insurance.ruleengine.domain.model.RuleStatus;
import com.insurance.ruleengine.domain.model.RuleTestCase;
import com.insurance.ruleengine.domain.model.RuleTestRun;
import com.insurance.ruleengine.domain.model.RuleTestSuite;
import com.insurance.ruleengine.domain.model.RuleVersion;
import com.insurance.ruleengine.domain.service.RuleTestAssertionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleEngineFacadeImplTest {
    @Test
    void createVersionLocksRuleBeforeAllocatingNextVersionAndReturnsCreatedVersion() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway,
                new NoopRuleExecutionGateway(),
                new NoopAuditGateway(),
                new PassThroughCryptoGateway(),
                new NoopRuleSetGateway(),
                new NoopApprovalFlowGateway());
        CreateRuleVersionCmd cmd = new CreateRuleVersionCmd();
        cmd.setDrlContent("package test;\nrule \"ok\" when then end\n");
        cmd.setVisualModel("{}");
        cmd.setCreatedBy("tester");

        RuleDTO dto = facade.createVersion("CI_UW_001", cmd);

        assertEquals(1, dto.getLatestVersion());
        assertEquals(1, ruleGateway.savedVersion.getVersion());
    }

    // ---- P0-3 approval callback tests ----

    @Test
    void handleApprovalCallback_approvesPendingVersion() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.savedVersion = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        ruleGateway.savedVersion.submitApproval();

        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new NoopApprovalFlowGateway());

        facade.handleApprovalCallback("CI_UW_001", 1, "APPROVED", "reviewer-a", "ok");

        assertEquals(RuleStatus.APPROVED, ruleGateway.savedVersion.getStatus());
        assertEquals("reviewer-a", ruleGateway.savedVersion.getApprovedBy());
    }

    @Test
    void handleApprovalCallback_rejectsPendingVersion() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.savedVersion = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        ruleGateway.savedVersion.submitApproval();

        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new NoopApprovalFlowGateway());

        facade.handleApprovalCallback("CI_UW_001", 1, "REJECTED", "reviewer-b", "declined");

        assertEquals(RuleStatus.REJECTED, ruleGateway.savedVersion.getStatus());
    }

    @Test
    void handleApprovalCallback_isIdempotentForRepeatedCallback() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.savedVersion = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        ruleGateway.savedVersion.submitApproval();

        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new NoopApprovalFlowGateway());

        facade.handleApprovalCallback("CI_UW_001", 1, "APPROVED", "r1", "first");
        facade.handleApprovalCallback("CI_UW_001", 1, "REJECTED", "r2", "duplicate must be no-op");

        assertEquals(RuleStatus.APPROVED, ruleGateway.savedVersion.getStatus());
        assertEquals("r1", ruleGateway.savedVersion.getApprovedBy());
    }

    @Test
    void handleApprovalCallback_rejectsUnknownStatus() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.savedVersion = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        ruleGateway.savedVersion.submitApproval();

        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new NoopApprovalFlowGateway());

        assertThrows(IllegalArgumentException.class,
                () -> facade.handleApprovalCallback("CI_UW_001", 1, "WAT", "r", "x"));
    }

    @Test
    void submitApproval_throwsWhenApprovalFlowFails() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.savedVersion = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        // record failure audit happened before rethrow
        AuditGateway[] auditCallCount = new AuditGateway[]{new NoopAuditGateway()};
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), auditCallCount[0],
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new FailingApprovalFlowGateway());

        SubmitApprovalCmd cmd = new SubmitApprovalCmd();
        cmd.setSubmittedBy("user");
        cmd.setReason("test");

        assertThrows(IllegalStateException.class, () -> facade.submitApproval("CI_UW_001", 1, cmd));
    }

    @Test
    void submitApproval_doesNotInvokeApprovalGatewayOrAdvanceStatus_whenAlreadyPending() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.savedVersion = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        ruleGateway.savedVersion.submitApproval();

        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new FailingApprovalFlowGateway());

        SubmitApprovalCmd cmd = new SubmitApprovalCmd();
        cmd.setSubmittedBy("user");
        cmd.setReason("test");

        // Should not throw even though approval gateway would fail, because it's already PENDING
        facade.submitApproval("CI_UW_001", 1, cmd);
        assertEquals(RuleStatus.PENDING_APPROVAL, ruleGateway.savedVersion.getStatus());
    }

    @Test
    void submitApproval_noopsWhenAlreadyPending() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.savedVersion = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        ruleGateway.savedVersion.submitApproval();

        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new FailingApprovalFlowGateway());

        SubmitApprovalCmd cmd = new SubmitApprovalCmd();
        cmd.setSubmittedBy("user");
        cmd.setReason("test");

        // Should not throw on the now-no-op path
        facade.submitApproval("CI_UW_001", 1, cmd);
        assertEquals(RuleStatus.PENDING_APPROVAL, ruleGateway.savedVersion.getStatus());
    }

    private static class FailingApprovalFlowGateway implements ApprovalFlowGateway {
        @Override
        public void submitApproval(String targetType, String targetId, String submittedBy, String reason) {
            throw new RuntimeException("approval-flow-service unavailable");
        }
    }

    private static class LockAwareRuleGateway implements RuleGateway {
        private final RuleDefinition rule = RuleDefinition.create("CI_UW_001", "name",
                RuleCategory.UNDERWRITING, "HEALTH", null, false, "owner", null);
        private boolean locked;
        private RuleVersion savedVersion;
        private java.util.List<RuleVersion> versions = java.util.Collections.emptyList();

        private LockAwareRuleGateway() {
        }

        @Override
        public boolean existsRule(String ruleCode) {
            return true;
        }

        @Override
        public Optional<RuleDefinition> findRule(String ruleCode) {
            return Optional.of(rule);
        }

        @Override
        public Optional<RuleDefinition> lockRuleForUpdate(String ruleCode) {
            locked = true;
            return Optional.of(rule);
        }

        @Override
        public RuleDefinition saveRule(RuleDefinition rule) {
            return rule;
        }

        @Override
        public int nextVersion(String ruleCode) {
            if (!locked) {
                throw new IllegalStateException("rule must be locked before allocating version");
            }
            return 1;
        }

        @Override
        public Optional<RuleVersion> findVersion(String ruleCode, Integer version) {
            for (RuleVersion v : versions) {
                if (v.getVersion().equals(version)) {
                    return Optional.of(v);
                }
            }
            // Fallback to savedVersion for backward compatibility
            if (savedVersion != null && savedVersion.getVersion().equals(version)) {
                return Optional.of(savedVersion);
            }
            return Optional.empty();
        }

        @Override
        public Optional<RuleVersion> findCurrentVersion(String ruleCode) {
            return Optional.ofNullable(savedVersion);
        }

        @Override
        public RuleVersion saveVersion(RuleVersion version) {
            savedVersion = version;
            return version;
        }

        @Override
        public List<RuleDefinition> listRules(String category, String businessLine, String status, String keyword) {
            return Collections.singletonList(rule);
        }

        @Override
        public List<RuleVersion> listVersions(String ruleCode) {
            return versions;
        }
    }

    private static class NoopRuleExecutionGateway implements RuleExecutionGateway {
        @Override
        public void validateDrl(String drlContent) {
        }

        @Override
        public ExecutionResult execute(RuleVersion version, ExecutionRequest request) {
            return new ExecutionResult();
        }
    }

    private static class NoopAuditGateway implements AuditGateway {
        @Override
        public void recordOperation(String ruleCode, Integer version, String action, String operator, String reason,
                                    String ipAddress) {
        }

        @Override
        public void recordOperation(String ruleCode, Integer version, String action, String operator, String reason,
                                    String ipAddress, String beforeJson, String afterJson) {
        }

        @Override
        public void recordExecution(ExecutionRequest request, ExecutionResult result) {
        }

        @Override
        public List<RuleExecutionLog> listExecutions(String ruleCode) {
            return Collections.emptyList();
        }

        @Override
        public List<RuleAuditLog> listAudits(String ruleCode) {
            return Collections.emptyList();
        }
    }

    private static class PassThroughCryptoGateway implements CryptoGateway {
        @Override
        public String encrypt(String plainText) {
            return plainText;
        }

        @Override
        public String decrypt(String cipherText) {
            return cipherText;
        }
    }

    private static class NoopRuleSetGateway implements RuleSetGateway {
        @Override
        public boolean existsRuleSet(String setCode) {
            return false;
        }

        @Override
        public Optional<RuleSet> findRuleSet(String setCode) {
            return Optional.empty();
        }

        @Override
        public RuleSet saveRuleSet(RuleSet ruleSet) {
            return ruleSet;
        }

        @Override
        public List<RuleSet> findAllRuleSets() {
            return Collections.emptyList();
        }

        @Override
        public RuleSet updateRuleSet(String setCode, RuleSet updated) {
            return updated;
        }

        @Override
        public void deleteRuleSet(String setCode) {
        }
    }

    private static class NoopCustomFieldGateway implements com.insurance.ruleengine.domain.gateway.CustomFieldGateway {
        @Override
        public List<com.insurance.ruleengine.domain.model.CustomField> findByBusinessLine(String businessLine) {
            return Collections.emptyList();
        }

        @Override
        public List<com.insurance.ruleengine.domain.model.CustomField> findAll() {
            return Collections.emptyList();
        }

        @Override
        public com.insurance.ruleengine.domain.model.CustomField save(com.insurance.ruleengine.domain.model.CustomField field) {
            return field;
        }

        @Override
        public void delete(Long id) {
        }
    }

    private static class NoopApprovalFlowGateway implements ApprovalFlowGateway {
        @Override
        public void submitApproval(String targetType, String targetId, String submittedBy, String reason) {
            // No-op for testing
        }
    }

    // ---- Additional facade tests for new methods ----

    @Test
    void publish_marksVersionAsPublished() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        RuleVersion v = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        v.submitApproval();
        v.markApproved("approver");
        ruleGateway.savedVersion = v;

        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new NoopApprovalFlowGateway());

        com.insurance.ruleengine.client.dto.PublishRuleCmd cmd = new com.insurance.ruleengine.client.dto.PublishRuleCmd();
        cmd.setVersion(1);
        cmd.setApprovedBy("approver");
        cmd.setGrayPercent(0);

        try {
            RuleDTO dto = facade.publish("CI_UW_001", cmd);
            assertNotNull(dto);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void publish_setsEffectiveWindowOnVersion() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        RuleVersion v = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        v.submitApproval();
        v.markApproved("approver");
        ruleGateway.savedVersion = v;

        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new NoopApprovalFlowGateway());

        LocalDateTime effectiveFrom = LocalDateTime.now().plusDays(1);
        LocalDateTime effectiveTo = effectiveFrom.plusDays(30);
        com.insurance.ruleengine.client.dto.PublishRuleCmd cmd = new com.insurance.ruleengine.client.dto.PublishRuleCmd();
        cmd.setVersion(1);
        cmd.setApprovedBy("approver");
        cmd.setGrayPercent(0);
        cmd.setEffectiveFrom(effectiveFrom);
        cmd.setEffectiveTo(effectiveTo);

        facade.publish("CI_UW_001", cmd);

        assertEquals(effectiveFrom, ruleGateway.savedVersion.getEffectiveFrom());
        assertEquals(effectiveTo, ruleGateway.savedVersion.getEffectiveTo());
    }

    @Test
    void publish_blocksWhenEnabledRuleTestSuiteFails() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        RuleVersion v = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        v.submitApproval();
        v.markApproved("approver");
        ruleGateway.savedVersion = v;
        FailingRuleTestGateway ruleTestGateway = new FailingRuleTestGateway();

        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new NoopApprovalFlowGateway());
        facade.setRuleTestGate(ruleTestGateway, new RuleTestAssertionService());

        com.insurance.ruleengine.client.dto.PublishRuleCmd cmd = new com.insurance.ruleengine.client.dto.PublishRuleCmd();
        cmd.setVersion(1);
        cmd.setApprovedBy("approver");
        cmd.setGrayPercent(0);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> facade.publish("CI_UW_001", cmd));

        assertEquals("APPROVED", ruleGateway.savedVersion.getStatus().name());
        assertEquals(1, ruleTestGateway.savedRuns.size());
        assertEquals("FAILED", ruleTestGateway.savedRuns.get(0).getStatus());
        assertEquals(true, error.getMessage().contains("test gate failed"));
    }

    @Test
    void rollback_revertsToTargetVersion() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        RuleVersion v1 = RuleVersion.draft("CI_UW_001", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        v1.submitApproval();
        v1.markApproved("a");
        RuleVersion v2 = RuleVersion.draft("CI_UW_001", 2,
                "package test; rule \"v2\" when then end", null, "c", "tester");
        v2.submitApproval();
        v2.markApproved("a");
        ruleGateway.savedVersion = v2;
        ruleGateway.versions = java.util.List.of(v1, v2);

        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new NoopApprovalFlowGateway());

        com.insurance.ruleengine.client.dto.RollbackRuleCmd cmd = new com.insurance.ruleengine.client.dto.RollbackRuleCmd();
        cmd.setTargetVersion(1);
        cmd.setOperator("tester");
        cmd.setReason("rollback test");

        try {
            RuleDTO dto = facade.rollback("CI_UW_001", cmd);
            assertNotNull(dto);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void archive_setsArchivedFlag() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway, new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new NoopApprovalFlowGateway());

        com.insurance.ruleengine.client.dto.ArchiveRuleCmd cmd = new com.insurance.ruleengine.client.dto.ArchiveRuleCmd();
        cmd.setOperator("admin");
        cmd.setReason("archive test");

        RuleDTO dto = facade.archive("CI_UW_001", cmd);
        assertEquals("CI_UW_001", dto.getRuleCode());
    }

    @Test
    void ruleSetOperations_createUpdateDelete() {
        InMemoryRuleSetGateway ruleSetGateway = new InMemoryRuleSetGateway();
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                new LockAwareRuleGateway(), new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), ruleSetGateway, new NoopApprovalFlowGateway());

        com.insurance.ruleengine.client.dto.CreateRuleSetCmd createCmd = new com.insurance.ruleengine.client.dto.CreateRuleSetCmd();
        createCmd.setSetCode("RS_001");
        createCmd.setSetName("Test Set");
        createCmd.setOwner("tester");
        createCmd.setSteps(Collections.emptyList());

        facade.createRuleSet(createCmd);
        facade.getRuleSet("RS_001");
        facade.updateRuleSet("RS_001", createCmd);
        facade.deleteRuleSet("RS_001");
        assertEquals(0, ruleSetGateway.store.size());
    }

    private static class InMemoryRuleSetGateway implements RuleSetGateway {
        java.util.Map<String, RuleSet> store = new java.util.HashMap<>();

        @Override
        public boolean existsRuleSet(String setCode) { return store.containsKey(setCode); }

        @Override
        public Optional<RuleSet> findRuleSet(String setCode) { return Optional.ofNullable(store.get(setCode)); }

        @Override
        public RuleSet saveRuleSet(RuleSet ruleSet) {
            store.put(ruleSet.getSetCode(), ruleSet);
            return ruleSet;
        }

        @Override
        public List<RuleSet> findAllRuleSets() { return new java.util.ArrayList<>(store.values()); }

        @Override
        public RuleSet updateRuleSet(String setCode, RuleSet updated) {
            store.put(setCode, updated);
            return updated;
        }

        @Override
        public void deleteRuleSet(String setCode) { store.remove(setCode); }
    }

    private static class FailingRuleTestGateway implements RuleTestGateway {
        private final RuleTestSuite suite = new RuleTestSuite();
        private final RuleTestCase testCase = new RuleTestCase();
        private final List<RuleTestRun> savedRuns = new java.util.ArrayList<>();

        private FailingRuleTestGateway() {
            suite.setSuiteCode("CI_UW_001_GATE");
            suite.setSuiteName("Publish gate");
            suite.setRuleCode("CI_UW_001");
            suite.setBusinessLine("HEALTH");
            suite.setEnabled(true);

            testCase.setCaseCode("CI_UW_001_CASE_FAIL");
            testCase.setCaseName("Reject unexpected accept");
            testCase.setRuleCode("CI_UW_001");
            testCase.setScenario("PUBLISH_GATE");
            testCase.setFactsJson("{}");
            testCase.setExpectedDecision("MANUAL_REVIEW");
            testCase.setExpectedHitRulesJson("[]");
            testCase.setExpectedOutputsJson("{}");
            testCase.setEnabled(true);
        }

        @Override
        public RuleTestCase saveCase(RuleTestCase testCase) { return testCase; }

        @Override
        public Optional<RuleTestCase> findCase(String caseCode) { return Optional.of(testCase); }

        @Override
        public List<RuleTestCase> listCases(String ruleCode, Boolean enabled) { return List.of(testCase); }

        @Override
        public void deleteCase(String caseCode) { }

        @Override
        public RuleTestSuite saveSuite(RuleTestSuite suite) { return suite; }

        @Override
        public Optional<RuleTestSuite> findSuite(String suiteCode) { return Optional.of(suite); }

        @Override
        public List<RuleTestSuite> listSuites(String ruleCode, String businessLine, Boolean enabled) {
            return List.of(suite);
        }

        @Override
        public void deleteSuite(String suiteCode) { }

        @Override
        public void addCaseToSuite(String suiteCode, String caseCode, int caseOrder) { }

        @Override
        public void removeCaseFromSuite(String suiteCode, String caseCode) { }

        @Override
        public List<RuleTestCase> listSuiteCases(String suiteCode) { return List.of(testCase); }

        @Override
        public RuleTestRun saveRun(RuleTestRun run) {
            savedRuns.add(run);
            return run;
        }

        @Override
        public Optional<RuleTestRun> findRun(String runId) {
            return savedRuns.stream().filter(run -> run.getRunId().equals(runId)).findFirst();
        }

        @Override
        public List<RuleTestRun> listRuns(String ruleCode, String suiteCode, String caseCode) {
            return savedRuns;
        }
    }

    @Test
    void customFields_crud() {
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                new LockAwareRuleGateway(), new NoopRuleExecutionGateway(), new NoopAuditGateway(),
                new PassThroughCryptoGateway(), new NoopRuleSetGateway(), new NoopApprovalFlowGateway(),
                new NoopCustomFieldGateway());

        facade.listCustomFields("HEALTH");
        facade.listAllCustomFields();

        com.insurance.ruleengine.client.dto.CustomFieldDTO field = new com.insurance.ruleengine.client.dto.CustomFieldDTO();
        field.setFieldCode("customAge");
        field.setFieldLabel("自定义年龄");
        field.setFieldType("number");
        field.setBusinessLine("HEALTH");
        field.setSortOrder(1);

        facade.createCustomField(field);
        facade.deleteCustomField(1L);
    }
}
