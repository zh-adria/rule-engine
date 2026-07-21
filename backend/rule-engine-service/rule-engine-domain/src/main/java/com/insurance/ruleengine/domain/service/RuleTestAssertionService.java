package com.insurance.ruleengine.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RuleTestAssertionService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> assertResult(RuleTestCase testCase, ExecutionResult result) {
        List<String> errors = new ArrayList<>();
        assertDecision(testCase, result, errors);
        assertHitRules(testCase, result, errors);
        assertOutputs(testCase, result, errors);
        return errors;
    }

    private void assertDecision(RuleTestCase testCase, ExecutionResult result, List<String> errors) {
        if (isBlank(testCase.getExpectedDecision())) {
            return;
        }
        String actual = result.getDecision() == null ? null : result.getDecision().name();
        if (!Objects.equals(testCase.getExpectedDecision(), actual)) {
            errors.add("decision expected " + testCase.getExpectedDecision() + " but was " + actual);
        }
    }

    private void assertHitRules(RuleTestCase testCase, ExecutionResult result, List<String> errors) {
        if (isBlank(testCase.getExpectedHitRulesJson())) {
            return;
        }
        List<String> expected = read(testCase.getExpectedHitRulesJson(), new TypeReference<>() {}, "expectedHitRulesJson");
        List<String> actual = result.getHitRules() == null ? List.of() : result.getHitRules();
        for (String hitRule : expected) {
            if (!actual.contains(hitRule)) {
                errors.add("hitRules missing expected rule " + hitRule);
            }
        }
    }

    private void assertOutputs(RuleTestCase testCase, ExecutionResult result, List<String> errors) {
        if (isBlank(testCase.getExpectedOutputsJson())) {
            return;
        }
        Map<String, Object> expected = read(testCase.getExpectedOutputsJson(), new TypeReference<>() {}, "expectedOutputsJson");
        Map<String, Object> actual = result.getOutputs() == null ? Map.of() : result.getOutputs();
        expected.forEach((key, expectedValue) -> {
            Object actualValue = actual.get(key);
            if (!Objects.equals(expectedValue, actualValue)) {
                errors.add("outputs." + key + " expected " + expectedValue + " but was " + actualValue);
            }
        });
    }

    private <T> T read(String json, TypeReference<T> type, String fieldName) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " is invalid JSON", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
