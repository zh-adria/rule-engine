package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class RollbackRuleCmd {
    @NotNull
    private Integer targetVersion;
    @NotBlank
    private String operator;
    @NotBlank
    private String reason;

    public Integer getTargetVersion() { return targetVersion; }
    public void setTargetVersion(Integer targetVersion) { this.targetVersion = targetVersion; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

