package com.insurance.ruleengine.client.dto;

/**
 * P1-1: request body for DRL ↔ visual-rule conversion.
 * Exactly one of {@code visualModel} or {@code drl} must be supplied.
 */
public class ConvertRuleRequest {
    /** When supplied, converted to DRL. */
    private Object visualModel;
    /** When supplied, parsed to visual model. */
    private String drl;

    public Object getVisualModel() { return visualModel; }
    public void setVisualModel(Object visualModel) { this.visualModel = visualModel; }
    public String getDrl() { return drl; }
    public void setDrl(String drl) { this.drl = drl; }
}
