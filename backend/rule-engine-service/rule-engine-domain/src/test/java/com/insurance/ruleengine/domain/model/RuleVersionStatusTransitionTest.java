package com.insurance.ruleengine.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * P2-1: RuleVersion lifecycle state-machine tests.
 */
class RuleVersionStatusTransitionTest {

    @Test
    void fullLifecycle() {
        RuleVersion v = RuleVersion.draft("R", 1, "package test; rule \"ok\" when then end", null, "c", "t");
        assertEquals(RuleStatus.DRAFT, v.getStatus());

        v.markTesting();
        assertEquals(RuleStatus.TESTING, v.getStatus());

        v.submitApproval();
        assertEquals(RuleStatus.PENDING_APPROVAL, v.getStatus());

        v.markApproved("reviewer");
        assertEquals(RuleStatus.APPROVED, v.getStatus());
        assertEquals("reviewer", v.getApprovedBy());

        v.publish("pub", false);
        assertEquals(RuleStatus.PUBLISHED, v.getStatus());
    }

    @Test
    void fullLifecycleWithGray() {
        RuleVersion v = RuleVersion.draft("R", 1, "package test; rule \"ok\" when then end", null, "c", "t");
        v.submitApproval();
        v.markApproved("r");
        v.publish("pub", true);
        assertEquals(RuleStatus.GRAY, v.getStatus());
    }

    @Test
    void canSubmitAgainAfterRejection() {
        RuleVersion v = RuleVersion.draft("R", 1, "package test; rule \"ok\" when then end", null, "c", "t");
        v.submitApproval();
        v.markRejected();
        assertEquals(RuleStatus.REJECTED, v.getStatus());

        v.submitApproval();
        assertEquals(RuleStatus.PENDING_APPROVAL, v.getStatus());
    }

    @Test
    void cannotPublishDraftDirectly() {
        RuleVersion v = RuleVersion.draft("R", 1, "package test; rule \"ok\" when then end", null, "c", "t");
        assertThrows(IllegalStateException.class, () -> v.publish("pub", false));
    }

    @Test
    void cannotRejectNonPending() {
        RuleVersion v = RuleVersion.draft("R", 1, "package test; rule \"ok\" when then end", null, "c", "t");
        assertThrows(IllegalStateException.class, v::markRejected);
    }

    @Test
    void rollback() {
        RuleVersion v = RuleVersion.draft("R", 1, "package test; rule \"ok\" when then end", null, "c", "t");
        v.submitApproval();
        v.markApproved("r");
        v.publish("p", false);
        assertEquals(RuleStatus.PUBLISHED, v.getStatus());

        v.rollback();
        assertEquals(RuleStatus.ROLLED_BACK, v.getStatus());
    }

    @Test
    void draftAfterPublishingNotAllowedToResubmitApproval() {
        RuleVersion v = RuleVersion.draft("R", 1, "package test; rule \"ok\" when then end", null, "c", "t");
        v.submitApproval();
        v.markApproved("r");
        v.publish("p", false);
        // published cannot go back to pending
        assertThrows(IllegalStateException.class, v::submitApproval);
    }
}
