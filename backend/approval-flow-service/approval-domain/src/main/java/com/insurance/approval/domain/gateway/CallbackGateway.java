package com.insurance.approval.domain.gateway;

public interface CallbackGateway {

    void notifyApprovalResult(String targetType, String targetId,
                              String status, String reviewedBy, String reason);
}
