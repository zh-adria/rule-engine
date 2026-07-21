package com.insurance.ruleengine.domain.service;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RulePolicyTest {
    private final RulePolicy policy = new RulePolicy();

    @Test
    void shouldAcceptSafeInput() {
        assertDoesNotThrow(() -> policy.validateRuleCode("CI_UW_001"));
        assertDoesNotThrow(() -> policy.validateFacts(Map.of("bmi", 32)));
    }

    @Test
    void shouldRejectUnsafeRuleCodeAndFacts() {
        assertThrows(IllegalArgumentException.class, () -> policy.validateRuleCode("rule;drop table"));
        assertThrows(IllegalArgumentException.class, () -> policy.validateFacts(Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> policy.validateFacts(Map.of("$eval", "bad")));
    }
}

