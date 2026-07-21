package com.insurance.ruleengine.client.dto;

import java.util.List;
import java.util.Map;

public class RuleSetExecutionResultDTO {
    private String traceId;
    private String setCode;
    private String decision;
    private List<String> hitRules;
    private Map<String, Object> outputs;
    private List<RuleSetStepResultDTO> stepResults;
    private long elapsedMs;

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public List<String> getHitRules() { return hitRules; }
    public void setHitRules(List<String> hitRules) { this.hitRules = hitRules; }
    public Map<String, Object> getOutputs() { return outputs; }
    public void setOutputs(Map<String, Object> outputs) { this.outputs = outputs; }
    public List<RuleSetStepResultDTO> getStepResults() { return stepResults; }
    public void setStepResults(List<RuleSetStepResultDTO> stepResults) { this.stepResults = stepResults; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
}
