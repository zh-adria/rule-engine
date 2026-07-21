package com.insurance.ruleengine.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * P2-4: security hardening tests (fact value validation).
 */
class RulePolicySecurityTest {

    private RulePolicy policy;

    @BeforeEach
    void setUp() {
        policy = new RulePolicy();
    }

    @Test
    void scriptTagRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validateFacts(Map.of("name", "<script>alert(1)</script>")));
    }

    @Test
    void sqlInjectionRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validateFacts(Map.of("code", "x'; DROP TABLE rules;--")));
    }

    @Test
    void normalValuesPass() {
        assertDoesNotThrow(() -> policy.validateFacts(Map.of("code", "CI2026", "age", 30)));
    }

    @Test
    void nonStringValuesSkipValueCheck() {
        // numbers, booleans, nested maps not subject to string blocklist
        Map<String, Object> facts = new HashMap<>();
        facts.put("count", 42);
        facts.put("enabled", true);
        facts.put("tags", java.util.List.of("a", "b"));
        assertDoesNotThrow(() -> policy.validateFacts(facts));
    }
}
