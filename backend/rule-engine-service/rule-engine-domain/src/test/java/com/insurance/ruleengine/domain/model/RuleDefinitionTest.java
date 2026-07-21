package com.insurance.ruleengine.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleDefinitionTest {
    @Test
    void shouldPublishFullVersion() {
        RuleDefinition rule = sampleRule();

        rule.publish(2, 100);

        assertEquals(2, rule.getCurrentVersion());
        assertNull(rule.getGrayVersion());
        assertEquals(0, rule.getGrayPercent());
    }

    @Test
    void shouldPublishGrayVersion() {
        RuleDefinition rule = sampleRule();
        rule.setCurrentVersion(1);

        rule.publish(2, 20);

        assertEquals(1, rule.getCurrentVersion());
        assertEquals(2, rule.getGrayVersion());
        assertEquals(20, rule.getGrayPercent());
    }

    @Test
    void shouldRejectInvalidGrayPercent() {
        RuleDefinition rule = sampleRule();

        assertThrows(IllegalArgumentException.class, () -> rule.publish(2, 101));
    }

    @Test
    void shouldClearGrayStateOnRollback() {
        RuleDefinition rule = sampleRule();
        rule.publish(2, 20);

        rule.rollback(1);

        assertEquals(1, rule.getCurrentVersion());
        assertNull(rule.getGrayVersion());
        assertEquals(0, rule.getGrayPercent());
    }

    @Test
    void archiveClearsGrayReleaseAndMarksRuleArchived() {
        RuleDefinition rule = RuleDefinition.create("CI_UW_HEALTH_2026", "重疾险健康告知核保规则",
                RuleCategory.UNDERWRITING, "CRITICAL_ILLNESS", "desc", false,
                "underwriting-team", "CBIRC-INSURANCE-SALES-TRACE");
        rule.publish(2, 30);

        rule.archive();

        assertTrue(rule.isArchived());
        assertNull(rule.getGrayVersion());
        assertEquals(0, rule.getGrayPercent());
    }

    @Test
    void newRuleIsNotArchived() {
        RuleDefinition rule = sampleRule();

        assertFalse(rule.isArchived());
    }

    private RuleDefinition sampleRule() {
        return RuleDefinition.create("CI_UW_001", "test", RuleCategory.UNDERWRITING,
                "CI", "test", false, "tester", "REG-001");
    }
}

