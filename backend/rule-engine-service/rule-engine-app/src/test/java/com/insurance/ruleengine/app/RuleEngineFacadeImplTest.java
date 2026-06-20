package com.insurance.ruleengine.app;

import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.domain.gateway.AuditGateway;
import com.insurance.ruleengine.domain.gateway.CryptoGateway;
import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.gateway.RuleGateway;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleCategory;
import com.insurance.ruleengine.domain.model.RuleDefinition;
import com.insurance.ruleengine.domain.model.RuleVersion;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static class LockAwareRuleGateway implements RuleGateway {
        private final RuleDefinition rule = RuleDefinition.create("CI_UW_001", "name",
                RuleCategory.UNDERWRITING, "HEALTH", null, false, "owner", null);
        private boolean locked;
        private RuleVersion savedVersion;

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
            return Optional.empty();
        }

        @Override
        public Optional<RuleVersion> findCurrentVersion(String ruleCode) {
            return Optional.empty();
        }

        @Override
        public RuleVersion saveVersion(RuleVersion version) {
            savedVersion = version;
            return version;
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
        public void recordExecution(ExecutionRequest request, ExecutionResult result) {
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
}
