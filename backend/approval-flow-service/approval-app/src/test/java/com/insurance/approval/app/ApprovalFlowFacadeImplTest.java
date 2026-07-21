package com.insurance.approval.app;

import com.insurance.approval.client.dto.ApprovalDTO;
import com.insurance.approval.client.dto.ReviewApprovalCmd;
import com.insurance.approval.client.dto.SubmitApprovalCmd;
import com.insurance.approval.domain.gateway.ApprovalGateway;
import com.insurance.approval.domain.gateway.ApprovalFlowGateway;
import com.insurance.approval.domain.gateway.CallbackGateway;
import com.insurance.approval.domain.model.ApprovalRecord;
import com.insurance.approval.domain.model.ApprovalStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalFlowFacadeImplTest {

    @Mock
    private ApprovalGateway approvalGateway;

    @Mock
    private ApprovalFlowGateway approvalFlowGateway;

    @Mock
    private CallbackGateway callbackGateway;

    @InjectMocks
    private ApprovalFlowFacadeImpl facade;

    @Test
    void submitApproval_createsPendingRecord() {
        SubmitApprovalCmd cmd = new SubmitApprovalCmd();
        cmd.setTargetType("RULE_VERSION");
        cmd.setTargetId("RULE:1");
        cmd.setSubmittedBy("alice");
        cmd.setReason("please review");

        when(approvalGateway.save(any(ApprovalRecord.class))).thenAnswer(inv -> {
            ApprovalRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        ApprovalDTO dto = facade.submitApproval(cmd);

        assertEquals(1L, dto.getId());
        assertEquals("PENDING", dto.getStatus());
        assertEquals("RULE:1", dto.getTargetId());

        ArgumentCaptor<ApprovalRecord> cap = ArgumentCaptor.forClass(ApprovalRecord.class);
        verify(approvalGateway).save(cap.capture());
        assertEquals(ApprovalStatus.PENDING, cap.getValue().getStatus());
    }

    @Test
    void approve_transitionsToApproved_andCallsCallback() {
        ApprovalRecord existing = ApprovalRecord.create("RULE_VERSION", "RULE:1", "alice", "please");
        existing.setId(1L);
        when(approvalGateway.findById(1L)).thenReturn(Optional.of(existing));
        when(approvalGateway.save(any(ApprovalRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewApprovalCmd cmd = new ReviewApprovalCmd();
        cmd.setReviewedBy("bob");
        cmd.setReason("looks good");

        ApprovalDTO dto = facade.approve(1L, cmd);

        assertEquals("APPROVED", dto.getStatus());
        assertEquals("bob", dto.getReviewedBy());

        verify(callbackGateway).notifyApprovalResult("RULE_VERSION", "RULE:1", "APPROVED", "bob", "looks good");
    }

    @Test
    void reject_transitionsToRejected_andCallsCallback() {
        ApprovalRecord existing = ApprovalRecord.create("RULE_VERSION", "RULE:2", "alice", "please");
        existing.setId(2L);
        when(approvalGateway.findById(2L)).thenReturn(Optional.of(existing));
        when(approvalGateway.save(any(ApprovalRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewApprovalCmd cmd = new ReviewApprovalCmd();
        cmd.setReviewedBy("bob");
        cmd.setReason("needs work");

        ApprovalDTO dto = facade.reject(2L, cmd);

        assertEquals("REJECTED", dto.getStatus());
        verify(callbackGateway).notifyApprovalResult("RULE_VERSION", "RULE:2", "REJECTED", "bob", "needs work");
    }

    @Test
    void approve_nonPendingRecord_throws() {
        ApprovalRecord existing = ApprovalRecord.create("RULE_VERSION", "RULE:1", "a", "r");
        existing.setId(1L);
        existing.approve("x", "y"); // now APPROVED
        when(approvalGateway.findById(1L)).thenReturn(Optional.of(existing));

        ReviewApprovalCmd cmd = new ReviewApprovalCmd();
        cmd.setReviewedBy("bob");
        cmd.setReason("ok");

        assertThrows(IllegalStateException.class, () -> facade.approve(1L, cmd));
        verify(callbackGateway, never()).notifyApprovalResult(any(), any(), any(), any(), any());
    }

    @Test
    void getApproval_notFound_throws() {
        when(approvalGateway.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> facade.getApproval(99L));
    }

    @Test
    void listApprovals_byStatus() {
        ApprovalRecord r1 = ApprovalRecord.create("RULE_VERSION", "R:1", "a", "r");
        r1.setId(1L);
        ApprovalRecord r2 = ApprovalRecord.create("RULE_VERSION", "R:2", "b", "r");
        r2.setId(2L);
        when(approvalGateway.findByStatus("PENDING")).thenReturn(List.of(r1, r2));

        List<ApprovalDTO> result = facade.listApprovals(null, null, "PENDING");
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(d -> "PENDING".equals(d.getStatus())));
    }
}
