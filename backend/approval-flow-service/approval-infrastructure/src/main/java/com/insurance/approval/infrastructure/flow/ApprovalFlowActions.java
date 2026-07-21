package com.insurance.approval.infrastructure.flow;

import com.insurance.approval.domain.model.ApprovalAction;
import com.insurance.approval.domain.model.ApprovalStatus;

public class ApprovalFlowActions {

    public String nextStatus(String currentStatus, String action) {
        ApprovalStatus current = ApprovalStatus.valueOf(currentStatus);
        ApprovalAction approvalAction = ApprovalAction.valueOf(action);

        if (approvalAction == ApprovalAction.SUBMIT && current == ApprovalStatus.PENDING) {
            return ApprovalStatus.PENDING.name();
        }
        if (approvalAction == ApprovalAction.APPROVE && current == ApprovalStatus.PENDING) {
            return ApprovalStatus.APPROVED.name();
        }
        if (approvalAction == ApprovalAction.REJECT && current == ApprovalStatus.PENDING) {
            return ApprovalStatus.REJECTED.name();
        }
        throw new IllegalStateException("invalid approval transition: " + current + " -> " + approvalAction);
    }
}
