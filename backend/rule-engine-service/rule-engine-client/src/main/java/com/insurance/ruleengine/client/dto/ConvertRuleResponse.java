package com.insurance.ruleengine.client.dto;

/**
 * P1-1: response body for DRL ↔ visual-rule conversion.
 * Exactly one of {@code drl} or {@code visualModel} is returned, depending on the request.
 */
public class ConvertRuleResponse {
    private String drl;
    private Object visualModel;

    public String getDrl() { return drl; }
    public void setDrl(String drl) { this.drl = drl; }
    public Object getVisualModel() { return visualModel; }
    public void setVisualModel(Object visualModel) { this.visualModel = visualModel; }
}
