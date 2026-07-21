package com.insurance.approval.client.dto;

import javax.validation.constraints.NotBlank;

public class ReviewApprovalCmd {

    @NotBlank(message = "审批人不能为空")
    private String reviewedBy;

    private String reason;

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
