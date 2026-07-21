package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.NotBlank;

public class SubmitApprovalCmd {
    @NotBlank
    private String submittedBy;
    private String reason;

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
