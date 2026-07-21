package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExecuteRuleCmd {
    @NotBlank
    private String ruleCode;
    private Integer version;
    @NotBlank
    private String scenario;
    @NotNull
    private Map<String, Object> facts = new LinkedHashMap<>();
    private String traceId;
    private String operator;

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public Map<String, Object> getFacts() { return facts; }
    public void setFacts(Map<String, Object> facts) { this.facts = facts; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}

