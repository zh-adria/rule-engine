package com.insurance.approval.client.dto;

import javax.validation.constraints.NotBlank;

public class SubmitApprovalCmd {

    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    @NotBlank(message = "目标ID不能为空")
    private String targetId;

    @NotBlank(message = "提交人不能为空")
    private String submittedBy;

    private String reason;

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
