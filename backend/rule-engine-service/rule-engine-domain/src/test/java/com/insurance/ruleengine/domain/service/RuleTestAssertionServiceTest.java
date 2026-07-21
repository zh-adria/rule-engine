package com.insurance.ruleengine.domain.service;

import com.insurance.ruleengine.domain.model.DecisionType;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleTestCase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleTestAssertionServiceTest {

    @Test
    void shouldPassWhenDecisionHitRulesAndOutputsMatch() {
        RuleTestCase testCase = new RuleTestCase();
        testCase.setExpectedDecision("ACCEPT");
        testCase.setExpectedHitRulesJson("[\"BMI_OK\"]");
        testCase.setExpectedOutputsJson("{\"riskLevel\":\"LOW\"}");

        ExecutionResult result = new ExecutionResult();
        result.setDecision(DecisionType.ACCEPT);
        result.setHitRules(List.of("BMI_OK", "AUDIT_RULE"));
        result.setOutputs(Map.of("riskLevel", "LOW", "message", "ok"));

        List<String> errors = new RuleTestAssertionService().assertResult(testCase, result);

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldReportDecisionHitRuleAndOutputMismatches() {
        RuleTestCase testCase = new RuleTestCase();
        testCase.setExpectedDecision("ACCEPT");
        testCase.setExpectedHitRulesJson("[\"BMI_OK\"]");
        testCase.setExpectedOutputsJson("{\"riskLevel\":\"LOW\"}");

        ExecutionResult result = new ExecutionResult();
        result.setDecision(DecisionType.MANUAL_REVIEW);
        result.setHitRules(List.of("BMI_REVIEW"));
        result.setOutputs(Map.of("riskLevel", "HIGH"));

        List<String> errors = new RuleTestAssertionService().assertResult(testCase, result);

        assertEquals(3, errors.size());
        assertTrue(errors.get(0).contains("decision"));
        assertTrue(errors.get(1).contains("hitRules"));
        assertTrue(errors.get(2).contains("outputs.riskLevel"));
    }

    @Test
    void shouldRejectInvalidExpectedJson() {
        RuleTestCase testCase = new RuleTestCase();
        testCase.setExpectedOutputsJson("{broken");

        ExecutionResult result = new ExecutionResult();
        result.setDecision(DecisionType.ACCEPT);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new RuleTestAssertionService().assertResult(testCase, result));

        assertTrue(error.getMessage().contains("expectedOutputsJson"));
    }
}
