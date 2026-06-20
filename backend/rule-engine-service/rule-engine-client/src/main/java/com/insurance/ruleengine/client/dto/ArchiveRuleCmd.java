package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.NotBlank;

public class ArchiveRuleCmd {
    @NotBlank
    private String operator;
    private String reason;

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
