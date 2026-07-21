package com.insurance.ruleengine.client.dto;

public class RuleSetStepDTO {
    private int stepOrder;
    private String ruleCode;
    private Integer ruleVersion;
    private String mode;
    private boolean stopOnDecline;

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(Integer ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isStopOnDecline() { return stopOnDecline; }
    public void setStopOnDecline(boolean stopOnDecline) { this.stopOnDecline = stopOnDecline; }
}
