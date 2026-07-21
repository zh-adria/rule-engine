package com.insurance.ruleengine.domain.model;

import java.util.Map;

public class ExecutionRequest {
    private String traceId;
    private String ruleCode;
    private Integer version;
    private String scenario;
    private Map<String, Object> facts;
    private String operator;

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public Map<String, Object> getFacts() { return facts; }
    public void setFacts(Map<String, Object> facts) { this.facts = facts; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}

