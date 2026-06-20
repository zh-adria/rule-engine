package com.insurance.ruleengine.infrastructure.drools;

import com.insurance.ruleengine.domain.model.DecisionType;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleVersion;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroolsRuleExecutionGatewayTest {
    private final DroolsRuleExecutionGateway gateway = new DroolsRuleExecutionGateway();

    @Test
    void shouldExecuteUnderwritingBranch() {
        String drl = "package test;\n"
                + "import java.util.Map;\n"
                + "import com.insurance.ruleengine.domain.model.ExecutionResult;\n"
                + "import com.insurance.ruleengine.domain.model.DecisionType;\n"
                + "rule \"high bmi\" when\n"
                + "  $facts: Map(this[\"bmi\"] != null, ((Number)this[\"bmi\"]).doubleValue() >= 32.0)\n"
                + "  $result: ExecutionResult()\n"
                + "then\n"
                + "  $result.setDecision(DecisionType.MANUAL_REVIEW);\n"
                + "  $result.getHitRules().add(\"high bmi\");\n"
                + "end\n";
        RuleVersion version = RuleVersion.draft("CI_UW_001", 1, drl, "{}", "checksum", "tester");
        ExecutionRequest request = new ExecutionRequest();
        request.setTraceId("trace-1");
        request.setRuleCode("CI_UW_001");
        request.setScenario("TEST");
        request.setFacts(Map.of("bmi", 33.0));

        ExecutionResult result = gateway.execute(version, request);

        assertEquals(DecisionType.MANUAL_REVIEW, result.getDecision());
        assertTrue(result.getHitRules().contains("high bmi"));
    }

    @Test
    void shouldRejectInvalidDrl() {
        assertThrows(IllegalArgumentException.class, () -> gateway.validateDrl("rule broken when then"));
    }

    @Test
    void shouldStopExecutionWhenMaxFiredRulesIsReached() {
        DroolsRuleExecutionGateway limitedGateway = new DroolsRuleExecutionGateway();
        limitedGateway.maxFiredRules = 1;
        String drl = "package test;\n"
                + "import java.util.Map;\n"
                + "import com.insurance.ruleengine.domain.model.ExecutionResult;\n"
                + "rule \"first\" when\n"
                + "  $facts: Map()\n"
                + "  $result: ExecutionResult()\n"
                + "then\n"
                + "  $result.getHitRules().add(\"first\");\n"
                + "end\n"
                + "rule \"second\" when\n"
                + "  $facts: Map()\n"
                + "  $result: ExecutionResult()\n"
                + "then\n"
                + "  $result.getHitRules().add(\"second\");\n"
                + "end\n";
        RuleVersion version = RuleVersion.draft("CI_UW_001", 1, drl, "{}", "limit-checksum", "tester");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> limitedGateway.execute(version, requestWithFacts(Map.of("bmi", 33.0))));

        assertTrue(error.getMessage().contains("maximum fired rules"));
    }

    @Test
    void shouldStopExecutionWhenTimeoutIsReached() {
        DroolsRuleExecutionGateway limitedGateway = new DroolsRuleExecutionGateway();
        limitedGateway.timeoutMs = 10;
        String drl = "package test;\n"
                + "import java.util.Map;\n"
                + "import com.insurance.ruleengine.domain.model.ExecutionResult;\n"
                + "rule \"slow\" when\n"
                + "  $facts: Map()\n"
                + "  $result: ExecutionResult()\n"
                + "then\n"
                + "  Thread.sleep(200L);\n"
                + "  $result.getHitRules().add(\"slow\");\n"
                + "end\n";
        RuleVersion version = RuleVersion.draft("CI_UW_001", 1, drl, "{}", "timeout-checksum", "tester");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> limitedGateway.execute(version, requestWithFacts(Map.of("bmi", 33.0))));

        assertTrue(error.getMessage().contains("timed out"));
    }

    @Test
    void shouldCacheCompiledRulesByRuleCodeVersionAndChecksum() {
        String drl = "package test;\n"
                + "import java.util.Map;\n"
                + "import com.insurance.ruleengine.domain.model.ExecutionResult;\n"
                + "import com.insurance.ruleengine.domain.model.DecisionType;\n"
                + "rule \"high bmi\" when\n"
                + "  $facts: Map(this[\"bmi\"] != null, ((Number)this[\"bmi\"]).doubleValue() >= 32.0)\n"
                + "  $result: ExecutionResult()\n"
                + "then\n"
                + "  $result.setDecision(DecisionType.MANUAL_REVIEW);\n"
                + "end\n";
        RuleVersion version = RuleVersion.draft("CI_UW_001", 1, drl, "{}", "checksum-1", "tester");
        ExecutionRequest request = new ExecutionRequest();
        request.setTraceId("trace-1");
        request.setRuleCode("CI_UW_001");
        request.setScenario("TEST");
        request.setFacts(Map.of("bmi", 33.0));

        gateway.execute(version, request);
        gateway.execute(version, request);

        assertEquals(1, gateway.cachedRuleCount());

        RuleVersion nextChecksum = RuleVersion.draft("CI_UW_001", 1, drl, "{}", "checksum-2", "tester");
        gateway.execute(nextChecksum, request);

        assertEquals(2, gateway.cachedRuleCount());
    }

    private ExecutionRequest requestWithFacts(Map<String, Object> facts) {
        ExecutionRequest request = new ExecutionRequest();
        request.setTraceId("trace-1");
        request.setRuleCode("CI_UW_001");
        request.setScenario("TEST");
        request.setFacts(facts);
        return request;
    }
}
