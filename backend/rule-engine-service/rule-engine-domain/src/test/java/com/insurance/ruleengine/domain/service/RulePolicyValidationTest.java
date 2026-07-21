package com.insurance.ruleengine.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * P2-1: RulePolicy validation tests.
 */
class RulePolicyValidationTest {

    private RulePolicy policy;

    @BeforeEach
    void setUp() {
        policy = new RulePolicy();
    }

    @Test
    void validRuleCodes() {
        assertDoesNotThrow(() -> policy.validateRuleCode("RULE_001"));
        assertDoesNotThrow(() -> policy.validateRuleCode("ABC"));
        assertDoesNotThrow(() -> policy.validateRuleCode("RULE-CODE-123"));
        assertDoesNotThrow(() -> policy.validateRuleCode("A".repeat(64)));
    }

    @Test
    void invalidRuleCodes() {
        assertThrows(IllegalArgumentException.class, () -> policy.validateRuleCode(null));
        assertThrows(IllegalArgumentException.class, () -> policy.validateRuleCode(""));
        assertThrows(IllegalArgumentException.class, () -> policy.validateRuleCode("AB")); // too short
        assertThrows(IllegalArgumentException.class, () -> policy.validateRuleCode("rule lower"));
        assertThrows(IllegalArgumentException.class, () -> policy.validateRuleCode("RULE.CODE"));
        assertThrows(IllegalArgumentException.class, () -> policy.validateRuleCode("RULE CODE"));
    }

    @Test
    void validFacts() {
        assertDoesNotThrow(() -> policy.validateFacts(Map.of("age", 30, "code", "X")));
    }

    @Test
    void emptyFactsRejected() {
        assertThrows(IllegalArgumentException.class, () -> policy.validateFacts(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> policy.validateFacts(null));
    }

    @Test
    void unsafeFactKeysRejected() {
        assertThrows(IllegalArgumentException.class, () -> policy.validateFacts(Map.of("obj.class", 1)));
        assertThrows(IllegalArgumentException.class, () -> policy.validateFacts(Map.of("obj$name", 1)));
        // null key: HashMap allows it, policy should reject
        Map<String, Object> m = new HashMap<>();
        m.put(null, 1);
        assertThrows(IllegalArgumentException.class, () -> policy.validateFacts(m));
        assertThrows(IllegalArgumentException.class, () -> policy.validateFacts(Map.of("x".repeat(65), 1)));
    }
}
