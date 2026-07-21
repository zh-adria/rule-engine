package com.insurance.ruleengine.domain.service;

import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.gateway.RuleGateway;
import com.insurance.ruleengine.domain.model.DecisionType;
import com.insurance.ruleengine.domain.model.ExecutionMode;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleDefinition;
import com.insurance.ruleengine.domain.model.RuleSet;
import com.insurance.ruleengine.domain.model.RuleSetStep;
import com.insurance.ruleengine.domain.model.RuleVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-2: verify RuleSetExecutor runs parallel steps concurrently (faster than serial would be).
 */
class RuleSetExecutorTest {

    @Test
    void parallelBatchExecutesConcurrently() {
        // two 200ms sleeps, parallel should finish ~200ms, serial ~400ms
        RuleExecutionGateway gateway = new SleepExecutionGateway(200);
        RuleGateway ruleGateway = new StubRuleGateway();

        RuleSetExecutor executor = new RuleSetExecutor(ruleGateway, gateway, (r, v) -> v);

        RuleSet ruleSet = RuleSet.create("S", "s", "d", "o");
        ruleSet.addStep(RuleSetStep.create("S", 1, "R1", 1, ExecutionMode.PARALLEL, false));
        ruleSet.addStep(RuleSetStep.create("S", 2, "R2", 1, ExecutionMode.PARALLEL, false));

        long start = System.currentTimeMillis();
        RuleSetExecutor.RuleSetOutput out = executor.execute(ruleSet, Map.of("x", 1), null, "op", "trace");
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(2, out.stepOutputs.size());
        assertFalse(out.stepOutputs.get(0).skipped);
        assertFalse(out.stepOutputs.get(1).skipped);
        // generous threshold: parallel should be < 350ms while serial would be > 380ms
        assertTrue(elapsed < 350, expectedParallelToRunConcurrently(elapsed));
    }

    @Test
    void serialStepsExecuteInOrder() {
        RuleSetExecutor executor = new RuleSetExecutor(new StubRuleGateway(),
                new NoopExecGateway(), (r, v) -> v);

        RuleSet ruleSet = RuleSet.create("S", "s", "d", "o");
        ruleSet.addStep(RuleSetStep.create("S", 1, "R1", 1, ExecutionMode.SERIAL, false));
        ruleSet.addStep(RuleSetStep.create("S", 2, "R2", 1, ExecutionMode.SERIAL, false));
        ruleSet.addStep(RuleSetStep.create("S", 3, "R3", 1, ExecutionMode.SERIAL, false));

        RuleSetExecutor.RuleSetOutput out = executor.execute(ruleSet, Map.of("x", 1), null, "op", "trace");

        assertEquals(3, out.stepOutputs.size());
        // worst-decision escalation: R2 is MANUAL_REVIEW, R3 is DECLINE → final = DECLINE
        assertEquals(DecisionType.DECLINE, out.finalDecision);
    }

    @Test
    void stopOnDeclineStopsSubsequentSteps() {
        RuleSetExecutor executor = new RuleSetExecutor(new StubRuleGateway(),
                new NoopExecGateway(), (r, v) -> v);

        RuleSet ruleSet = RuleSet.create("S", "s", "d", "o");
        // R2 with stopOnDecline, marked DECLINE
        ruleSet.addStep(RuleSetStep.create("S", 1, "R1", 1, ExecutionMode.SERIAL, false));
        ruleSet.addStep(RuleSetStep.create("S", 2, "R2_STOP", 1, ExecutionMode.SERIAL, true));
        ruleSet.addStep(RuleSetStep.create("S", 3, "R3_NEVER", 1, ExecutionMode.SERIAL, false));

        RuleSetExecutor.RuleSetOutput out = executor.execute(ruleSet, Map.of("x", 1), null, "op", "trace");

        assertEquals(3, out.stepOutputs.size());
        assertFalse(out.stepOutputs.get(0).skipped);
        assertFalse(out.stepOutputs.get(1).skipped);
        assertTrue(out.stepOutputs.get(2).skipped);
    }

    private String expectedParallelToRunConcurrently(long elapsed) {
        return "parallel batch took " + elapsed + "ms, expected < 350ms";
    }

    private static class SleepExecutionGateway implements RuleExecutionGateway {
        private final int millis;

        SleepExecutionGateway(int millis) {
            this.millis = millis;
        }

        @Override
        public void validateDrl(String drlContent) {
        }

        @Override
        public ExecutionResult execute(RuleVersion version, ExecutionRequest request) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ExecutionResult r = new ExecutionResult();
            r.setDecision(DecisionType.ACCEPT);
            r.getHitRules().add(version.getRuleCode());
            return r;
        }
    }

    private static class NoopExecGateway implements RuleExecutionGateway {
        @Override
        public void validateDrl(String drlContent) {
        }

        @Override
        public ExecutionResult execute(RuleVersion version, ExecutionRequest request) {
            ExecutionResult r = new ExecutionResult();
            // simulate different decisions depending on rule
            if (version.getRuleCode() != null && version.getRuleCode().contains("R2_STOP")) {
                r.setDecision(DecisionType.DECLINE);
            } else if (version.getRuleCode() != null && version.getRuleCode().contains("R2")) {
                r.setDecision(DecisionType.MANUAL_REVIEW);
            } else if (version.getRuleCode() != null && version.getRuleCode().contains("R3")) {
                r.setDecision(DecisionType.DECLINE);
            } else {
                r.setDecision(DecisionType.ACCEPT);
            }
            r.getHitRules().add(version.getRuleCode());
            return r;
        }
    }

    private static class StubRuleGateway implements RuleGateway {
        private final RuleDefinition rule1 = rule("R1");
        private final RuleDefinition rule2 = rule("R2");
        private final RuleDefinition rule2stop = rule("R2_STOP");
        private final RuleDefinition rule3 = rule("R3");
        private final RuleDefinition rule3never = rule("R3_NEVER");
        private final RuleVersion version = RuleVersion.draft("X", 1,
                "package test; rule \"ok\" when then end", null, "c", "tester");
        {
            version.setRuleCode(null); // let tests override via reflection? No — we use a copy per execute
        }

        private RuleDefinition rule(String code) {
            RuleDefinition r = RuleDefinition.create(code, code,
                    com.insurance.ruleengine.domain.model.RuleCategory.UNDERWRITING,
                    "LIFE", null, false, "o", null);
            return r;
        }

        @Override
        public boolean existsRule(String ruleCode) {
            return true;
        }

        @Override
        public Optional<RuleDefinition> findRule(String ruleCode) {
            return Optional.of(newRule(ruleCode));
        }

        @Override
        public Optional<RuleDefinition> lockRuleForUpdate(String ruleCode) {
            return Optional.of(newRule(ruleCode));
        }

        @Override
        public RuleDefinition saveRule(RuleDefinition rule) {
            return rule;
        }

        @Override
        public int nextVersion(String ruleCode) {
            return 1;
        }

        @Override
        public Optional<RuleVersion> findVersion(String ruleCode, Integer version) {
            RuleVersion v = RuleVersion.draft(ruleCode, version,
                    "package test; rule \"ok\" when then end", null, "c", "tester");
            return Optional.of(v);
        }

        @Override
        public Optional<RuleVersion> findCurrentVersion(String ruleCode) {
            return findVersion(ruleCode, 1);
        }

        @Override
        public RuleVersion saveVersion(RuleVersion version) {
            return version;
        }

        @Override
        public List<RuleDefinition> listRules(String c, String b, String s, String k) {
            return List.of();
        }

        @Override
        public List<RuleVersion> listVersions(String ruleCode) {
            return List.of(findCurrentVersion(ruleCode).orElseThrow());
        }

        private RuleDefinition newRule(String ruleCode) {
            return RuleDefinition.create(ruleCode, ruleCode,
                    com.insurance.ruleengine.domain.model.RuleCategory.UNDERWRITING,
                    "LIFE", null, false, "o", null);
        }
    }
}
