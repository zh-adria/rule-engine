package com.insurance.approval.domain.service;

public class ApprovalPolicy {

    public void validateTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            throw new IllegalArgumentException("目标类型不能为空");
        }
    }

    public void validateTargetId(String targetId) {
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("目标ID不能为空");
        }
    }

    public void validateSubmittedBy(String submittedBy) {
        if (submittedBy == null || submittedBy.isBlank()) {
            throw new IllegalArgumentException("提交人不能为空");
        }
    }

    public void validateReviewedBy(String reviewedBy) {
        if (reviewedBy == null || reviewedBy.isBlank()) {
            throw new IllegalArgumentException("审批人不能为空");
        }
    }
}
