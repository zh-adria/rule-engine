package com.insurance.ruleengine.infrastructure.drools;

import com.insurance.ruleengine.domain.model.DecisionType;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-2: verifies that Drools rules can set decision + hitRules on ExecutionResult,
 * and that the gateway surfaces them back to the caller.
 */
class DroolsDecisionAndHitRulesTest {

    private DroolsRuleExecutionGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new DroolsRuleExecutionGateway();
    }

    @Test
    void defaultDecisionIsAcceptWhenNothingFires() {
        // empty consequence: decision should remain ACCEPT
        RuleVersion v = drlRule("package insurance.test\n" +
                "import com.insurance.ruleengine.domain.model.ExecutionResult\n" +
                "rule \"no-op\"\n" +
                "when\n" +
                "  $result: ExecutionResult()\n" +
                "then\n" +
                "  // no-op\n" +
                "end");

        ExecutionResult result = gateway.execute(v, request(Map.of("x", 1)));

        assertEquals(DecisionType.ACCEPT, result.getDecision());
        assertNotNull(result.getHitRules());
    }

    @Test
    void ruleCanSetDeclineDecision() {
        RuleVersion v = drlRule(
                "package insurance.test\n" +
                        "import java.util.Map\n" +
                        "import com.insurance.ruleengine.domain.model.ExecutionResult\n" +
                        "import com.insurance.ruleengine.domain.model.DecisionType\n" +
                        "rule \"decline-high-risk\"\n" +
                        "when\n" +
                        "  $facts: Map(this[\"risk\"] == \"high\")\n" +
                        "  $result: ExecutionResult()\n" +
                        "then\n" +
                        "  $result.setDecision(DecisionType.DECLINE);\n" +
                        "  $result.getOutputs().put(\"reason\", \"risk too high\");\n" +
                        "end");

        ExecutionResult result = gateway.execute(v, request(Map.of("risk", "high")));

        assertEquals(DecisionType.DECLINE, result.getDecision());
        assertTrue(result.getElapsedMs() >= 0);
    }

    @Test
    void invalidDrlThrowsOnValidate() {
        assertThrows(IllegalArgumentException.class, () -> gateway.validateDrl("this is not valid drools"));
    }

    private RuleVersion drlRule(String drl) {
        return RuleVersion.draft("TEST", 1, drl, null, "checksum", "tester");
    }

    private ExecutionRequest request(Map<String, Object> facts) {
        ExecutionRequest r = new ExecutionRequest();
        r.setRuleCode("TEST");
        r.setVersion(1);
        r.setTraceId("trace-1");
        r.setOperator("tester");
        Map<String, Object> f = new HashMap<>(facts);
        r.setFacts(f);
        return r;
    }
}
