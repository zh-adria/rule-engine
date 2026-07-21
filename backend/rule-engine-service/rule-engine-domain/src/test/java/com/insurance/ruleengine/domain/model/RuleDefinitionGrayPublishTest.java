package com.insurance.ruleengine.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for RuleDefinition.publish() gray-release semantics.
 * P0-1: gray version routing state-machine correctness.
 */
class RuleDefinitionGrayPublishTest {

    private RuleDefinition rule() {
        return RuleDefinition.create("UNDERWRITE_001", "underwriting", RuleCategory.UNDERWRITING,
                "life", "demo", false, "tester", "REF-001");
    }

    @Test
    void fullRolloutSetsCurrentAndClearsGray() {
        RuleDefinition rule = rule();
        rule.publish(1, 100);

        assertEquals(1, rule.getCurrentVersion());
        assertNull(rule.getGrayVersion());
        assertEquals(0, rule.getGrayPercent());
    }

    @Test
    void grayRolloutSetsGrayVersionAndKeepsCurrent() {
        RuleDefinition rule = rule();
        rule.publish(1, 100);                 // first full rollout v1
        rule.publish(2, 30);                  // now gray v2 at 30%

        assertEquals(1, rule.getCurrentVersion()); // v1 still current
        assertEquals(2, rule.getGrayVersion());
        assertEquals(30, rule.getGrayPercent());
    }

    @Test
    void grayRolloutRejectsSecondGrayOnDifferentVersion() {
        RuleDefinition rule = rule();
        rule.publish(2, 30);

        assertThrows(IllegalStateException.class, () -> rule.publish(3, 50));
        assertEquals(2, rule.getGrayVersion());  // unchanged
    }

    @Test
    void grayRolloutIsIdempotentForSameVersionAndPercent() {
        RuleDefinition rule = rule();
        rule.publish(2, 30);
        rule.publish(2, 30);                  // no-op

        assertEquals(2, rule.getGrayVersion());
        assertEquals(30, rule.getGrayPercent());
    }

    @Test
    void fullRolloutAfterGrayReplacesGrayWithCurrent() {
        RuleDefinition rule = rule();
        rule.publish(1, 100);
        rule.publish(2, 30);
        rule.publish(2, 100);                 // promote to full

        assertEquals(2, rule.getCurrentVersion());
        assertNull(rule.getGrayVersion());
        assertEquals(0, rule.getGrayPercent());
    }

    @Test
    void invalidPercentThrows() {
        assertThrows(IllegalArgumentException.class, () -> rule().publish(1, -1));
        assertThrows(IllegalArgumentException.class, () -> rule().publish(1, 101));
    }

    @Test
    void percentZeroIsFullRollout() {
        RuleDefinition rule = rule();
        rule.publish(1, 0);

        assertEquals(1, rule.getCurrentVersion());
        assertNull(rule.getGrayVersion());
    }

    @Test
    void rollbackClearsGray() {
        RuleDefinition rule = rule();
        rule.publish(1, 100);
        rule.publish(2, 30);
        rule.rollback(1);

        assertEquals(1, rule.getCurrentVersion());
        assertNull(rule.getGrayVersion());
        assertEquals(0, rule.getGrayPercent());
    }
}
