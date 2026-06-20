package com.insurance.ruleengine.client.dto;

import java.time.LocalDateTime;

public class RuleExecutionLogDTO {
    private String traceId;
    private String ruleCode;
    private Integer version;
    private String scenario;
    private String decision;
    private String hitRules;
    private long elapsedMs;
    private String operator;
    private LocalDateTime createdAt;

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getHitRules() { return hitRules; }
    public void setHitRules(String hitRules) { this.hitRules = hitRules; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
