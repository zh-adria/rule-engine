package com.insurance.ruleengine.app;

import com.insurance.ruleengine.client.dto.ArchiveRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.PublishRuleCmd;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleVersionDTO;
import com.insurance.ruleengine.domain.gateway.AuditGateway;
import com.insurance.ruleengine.domain.gateway.CryptoGateway;
import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.gateway.RuleGateway;
import com.insurance.ruleengine.domain.model.DecisionType;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleCategory;
import com.insurance.ruleengine.domain.model.RuleAuditLog;
import com.insurance.ruleengine.domain.model.RuleDefinition;
import com.insurance.ruleengine.domain.model.RuleExecutionLog;
import com.insurance.ruleengine.domain.model.RuleVersion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineFacadeImplTest {
    @Test
    void createVersionLocksRuleBeforeAllocatingNextVersionAndReturnsCreatedVersion() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway,
                new NoopRuleExecutionGateway(),
                new NoopAuditGateway(),
                new PassThroughCryptoGateway());
        CreateRuleVersionCmd cmd = new CreateRuleVersionCmd();
        cmd.setDrlContent("package test;\nrule \"ok\" when then end\n");
        cmd.setVisualModel("{}");
        cmd.setCreatedBy("tester");

        RuleDTO dto = facade.createVersion("CI_UW_001", cmd);

        assertEquals(1, dto.getLatestVersion());
        assertEquals(1, ruleGateway.savedVersion.getVersion());
    }

    @Test
    void listRulesFiltersByCategoryAndKeyword() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.rules.add(RuleDefinition.create("FRAUD_BLACKLIST_2026", "反欺诈黑名单",
                RuleCategory.RISK_CONTROL, "ONLINE_APPLICATION", null, false, "risk", null));
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway,
                new NoopRuleExecutionGateway(),
                new NoopAuditGateway(),
                new PassThroughCryptoGateway());

        List<RuleDTO> rules = facade.listRules("UNDERWRITING", null, null, "CI");

        assertEquals(1, rules.size());
        assertEquals("CI_UW_001", rules.get(0).getRuleCode());
    }

    @Test
    void listVersionsReturnsNewestFirst() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.versions.add(RuleVersion.draft("CI_UW_001", 1, "package test;", "{}", "v1", "tester"));
        ruleGateway.versions.add(RuleVersion.draft("CI_UW_001", 2, "package test;", "{}", "v2", "tester"));
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway,
                new NoopRuleExecutionGateway(),
                new NoopAuditGateway(),
                new PassThroughCryptoGateway());

        List<RuleVersionDTO> versions = facade.listVersions("CI_UW_001");

        assertEquals(2, versions.size());
        assertEquals(2, versions.get(0).getVersion());
        assertEquals("v2", versions.get(0).getChecksum());
    }

    @Test
    void archivePersistsStateAndWritesAudit() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        NoopAuditGateway auditGateway = new NoopAuditGateway();
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway,
                new NoopRuleExecutionGateway(),
                auditGateway,
                new PassThroughCryptoGateway());

        RuleDTO archived = facade.archive("CI_UW_001", archiveCmd("admin", "retired"));

        assertTrue(archived.isArchived());
        assertTrue(ruleGateway.rule.isArchived());
        assertEquals("ARCHIVE", auditGateway.lastAction);
        assertEquals("retired", auditGateway.lastReason);
    }

    @Test
    void archivedRuleCannotBePublished() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.rule.archive();
        ruleGateway.versions.add(RuleVersion.draft("CI_UW_001", 1, "package test;", "{}", "v1", "tester"));
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway,
                new NoopRuleExecutionGateway(),
                new NoopAuditGateway(),
                new PassThroughCryptoGateway());

        PublishRuleCmd publish = new PublishRuleCmd();
        publish.setVersion(1);
        publish.setApprovedBy("admin");
        publish.setGrayPercent(0);

        assertThrows(IllegalStateException.class, () -> facade.publish("CI_UW_001", publish));
    }

    @Test
    void archivedRuleCannotBeExecuted() {
        LockAwareRuleGateway ruleGateway = new LockAwareRuleGateway();
        ruleGateway.rule.archive();
        RuleEngineFacadeImpl facade = new RuleEngineFacadeImpl(
                ruleGateway,
                new NoopRuleExecutionGateway(),
                new NoopAuditGateway(),
                new PassThroughCryptoGateway());

        ExecuteRuleCmd cmd = new ExecuteRuleCmd();
        cmd.setRuleCode("CI_UW_001");
        cmd.setScenario("UNDERWRITING");

        assertThrows(IllegalStateException.class, () -> facade.execute(cmd));
    }

    private static class LockAwareRuleGateway implements RuleGateway {
        private final RuleDefinition rule = RuleDefinition.create("CI_UW_001", "name",
                RuleCategory.UNDERWRITING, "HEALTH", null, false, "owner", null);
        private final List<RuleDefinition> rules = new ArrayList<>();
        private final List<RuleVersion> versions = new ArrayList<>();
        private boolean locked;
        private RuleVersion savedVersion;

        private LockAwareRuleGateway() {
            rules.add(rule);
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
            return versions.stream()
                    .filter(item -> item.getRuleCode().equals(ruleCode) && item.getVersion().equals(version))
                    .findFirst();
        }

        @Override
        public Optional<RuleVersion> findCurrentVersion(String ruleCode) {
            return Optional.empty();
        }

        @Override
        public RuleVersion saveVersion(RuleVersion version) {
            savedVersion = version;
            versions.add(version);
            return version;
        }

        @Override
        public List<RuleDefinition> listRules(String category, String businessLine, String status, String keyword) {
            return rules.stream()
                    .filter(item -> category == null || item.getCategory().name().equals(category))
                    .filter(item -> businessLine == null || item.getBusinessLine().equals(businessLine))
                    .filter(item -> keyword == null || item.getRuleCode().contains(keyword) || item.getRuleName().contains(keyword))
                    .toList();
        }

        @Override
        public List<RuleVersion> listVersions(String ruleCode) {
            return versions.stream()
                    .filter(item -> item.getRuleCode().equals(ruleCode))
                    .sorted((left, right) -> right.getVersion().compareTo(left.getVersion()))
                    .toList();
        }
    }

    private static class NoopRuleExecutionGateway implements RuleExecutionGateway {
        @Override
        public void validateDrl(String drlContent) {
        }

        @Override
        public ExecutionResult execute(RuleVersion version, ExecutionRequest request) {
            ExecutionResult result = new ExecutionResult();
            result.setTraceId(request.getTraceId());
            result.setRuleCode(request.getRuleCode());
            result.setVersion(version.getVersion());
            result.setDecision(DecisionType.ACCEPT);
            return result;
        }
    }

    private static class NoopAuditGateway implements AuditGateway {
        private String lastAction;
        private String lastReason;

        @Override
        public void recordOperation(String ruleCode, Integer version, String action, String operator, String reason,
                                    String ipAddress) {
            lastAction = action;
            lastReason = reason;
        }

        @Override
        public void recordExecution(ExecutionRequest request, ExecutionResult result) {
        }

        @Override
        public List<RuleExecutionLog> listExecutions(String ruleCode) {
            return List.of();
        }

        @Override
        public List<RuleAuditLog> listAudits(String ruleCode) {
            return List.of();
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

    private ArchiveRuleCmd archiveCmd(String operator, String reason) {
        ArchiveRuleCmd cmd = new ArchiveRuleCmd();
        cmd.setOperator(operator);
        cmd.setReason(reason);
        return cmd;
    }
}
