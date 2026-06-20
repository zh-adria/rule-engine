package com.insurance.ruleengine.domain.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExecutionResult {
    private String traceId;
    private String ruleCode;
    private Integer version;
    private DecisionType decision = DecisionType.ACCEPT;
    private List<String> hitRules = new ArrayList<>();
    private Map<String, Object> outputs = new LinkedHashMap<>();
    private long elapsedMs;

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public DecisionType getDecision() { return decision; }
    public void setDecision(DecisionType decision) { this.decision = decision; }
    public List<String> getHitRules() { return hitRules; }
    public void setHitRules(List<String> hitRules) { this.hitRules = hitRules; }
    public Map<String, Object> getOutputs() { return outputs; }
    public void setOutputs(Map<String, Object> outputs) { this.outputs = outputs; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
}

