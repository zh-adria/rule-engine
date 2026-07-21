package com.insurance.ruleengine.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2-1: RuleSet / RuleSetStep behavior tests.
 */
class RuleSetStepTest {

    @Test
    void createRuleSet() {
        RuleSet rs = RuleSet.create("S", "My Set", "demo", "owner");
        assertEquals("S", rs.getSetCode());
        assertEquals("My Set", rs.getSetName());
        assertEquals(0, rs.getSteps().size());
    }

    @Test
    void addStepsAndRetainOrder() {
        RuleSet rs = RuleSet.create("S", "s", "d", "o");
        rs.addStep(RuleSetStep.create("S", 1, "R1", null, ExecutionMode.SERIAL, false));
        rs.addStep(RuleSetStep.create("S", 2, "R2", null, ExecutionMode.PARALLEL, false));

        List<RuleSetStep> steps = rs.getSteps();
        assertEquals(2, steps.size());
        assertEquals("R1", steps.get(0).getRuleCode());
        assertEquals("R2", steps.get(1).getRuleCode());
    }

    @Test
    void serializerStepAttributes() {
        RuleSetStep step = RuleSetStep.create("S", 1, "R1", 5, ExecutionMode.SERIAL, true);
        assertEquals(1, step.getStepOrder());
        assertEquals("R1", step.getRuleCode());
        assertEquals(Integer.valueOf(5), step.getRuleVersion());
        assertEquals(ExecutionMode.SERIAL, step.getMode());
        assertTrue(step.isStopOnDecline());
    }
}
