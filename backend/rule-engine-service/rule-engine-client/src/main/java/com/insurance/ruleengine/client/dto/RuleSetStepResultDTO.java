package com.insurance.ruleengine.client.dto;

public class RuleSetStepResultDTO {
    private int stepOrder;
    private String ruleCode;
    private Integer version;
    private String decision;
    private boolean skipped;

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public boolean isSkipped() { return skipped; }
    public void setSkipped(boolean skipped) { this.skipped = skipped; }
}
