package com.insurance.ruleengine.infrastructure.drools;

import com.insurance.ruleengine.domain.model.DecisionType;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-4: verify AgendaEventListener populates ExecutionResult.hitRules.
 */
class DroolsHitRulesTest {

    private DroolsRuleExecutionGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new DroolsRuleExecutionGateway();
    }

    @Test
    void firedRulesAreTracked() {
        RuleVersion v = drlRule("package insurance.test\n" +
                "import java.util.Map\n" +
                "import com.insurance.ruleengine.domain.model.ExecutionResult\n" +
                "import com.insurance.ruleengine.domain.model.DecisionType\n" +
                "rule \"rule-1\"\n" +
                "when\n" +
                "  $facts: Map(this[\"x\"] == 1)\n" +
                "  $result: ExecutionResult()\n" +
                "then\n" +
                "  $result.setDecision(DecisionType.ACCEPT);\n" +
                "end\n" +
                "rule \"rule-2\"\n" +
                "when\n" +
                "  $facts: Map(this[\"x\"] == 1)\n" +
                "  $result: ExecutionResult()\n" +
                "then\n" +
                "  $result.setDecision(DecisionType.MANUAL_REVIEW);\n" +
                "end");

        ExecutionResult result = gateway.execute(v, request(Map.of("x", 1)));

        assertTrue(result.getHitRules().contains("rule-1"));
        assertTrue(result.getHitRules().contains("rule-2"));
        assertEquals(2, result.getHitRules().size());
    }

    @Test
    void unmatchedRuleNotInHitRules() {
        RuleVersion v = drlRule("package insurance.test\n" +
                "import java.util.Map\n" +
                "rule \"matches-when-two\"\n" +
                "when\n" +
                "  Map(this[\"x\"] == 2)\n" +
                "then\n" +
                "end\n" +
                "rule \"matches-when-one\"\n" +
                "when\n" +
                "  Map(this[\"x\"] == 1)\n" +
                "then\n" +
                "end");

        ExecutionResult result = gateway.execute(v, request(Map.of("x", 1)));

        assertEquals(List.of("matches-when-one"), result.getHitRules());
    }

    @Test
    void noRulesFireWhenNoFactsMatch() {
        RuleVersion v = drlRule("package insurance.test\n" +
                "import java.util.Map\n" +
                "rule \"high-threshold\"\n" +
                "when\n" +
                "  Map(this[\"x\"] > 1000)\n" +
                "then\n" +
                "end");

        ExecutionResult result = gateway.execute(v, request(Map.of("x", 1)));

        assertTrue(result.getHitRules().isEmpty());
    }

    private RuleVersion drlRule(String drl) {
        return RuleVersion.draft("T", 1, drl, null, "c", "tester");
    }

    private ExecutionRequest request(Map<String, Object> facts) {
        ExecutionRequest r = new ExecutionRequest();
        r.setRuleCode("T");
        r.setVersion(1);
        r.setTraceId("t-1");
        r.setOperator("tester");
        Map<String, Object> f = new HashMap<>(facts);
        r.setFacts(f);
        return r;
    }
}
