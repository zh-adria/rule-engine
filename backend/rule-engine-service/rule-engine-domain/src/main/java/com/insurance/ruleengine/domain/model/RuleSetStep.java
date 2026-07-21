package com.insurance.ruleengine.domain.model;

public class RuleSetStep {
    private Long id;
    private String setCode;
    private int stepOrder;
    private String ruleCode;
    private Integer ruleVersion;
    private ExecutionMode mode;
    private boolean stopOnDecline;

    public static RuleSetStep create(String setCode, int stepOrder, String ruleCode, Integer ruleVersion,
                                     ExecutionMode mode, boolean stopOnDecline) {
        RuleSetStep step = new RuleSetStep();
        step.setCode = setCode;
        step.stepOrder = stepOrder;
        step.ruleCode = ruleCode;
        step.ruleVersion = ruleVersion;
        step.mode = mode;
        step.stopOnDecline = stopOnDecline;
        return step;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(Integer ruleVersion) { this.ruleVersion = ruleVersion; }
    public ExecutionMode getMode() { return mode; }
    public void setMode(ExecutionMode mode) { this.mode = mode; }
    public boolean isStopOnDecline() { return stopOnDecline; }
    public void setStopOnDecline(boolean stopOnDecline) { this.stopOnDecline = stopOnDecline; }
}
