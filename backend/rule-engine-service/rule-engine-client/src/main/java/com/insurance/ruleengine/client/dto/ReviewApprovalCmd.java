package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.NotBlank;

public class ReviewApprovalCmd {
    @NotBlank
    private String reviewedBy;
    private String reason;

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
