package com.insurance.ruleengine.client.dto;

import java.time.LocalDateTime;

public class RuleTestCaseDTO {
    private Long id;
    private String caseCode;
    private String caseName;
    private String ruleCode;
    private Integer version;
    private String scenario;
    private String factsJson;
    private String expectedDecision;
    private String expectedHitRulesJson;
    private String expectedOutputsJson;
    private boolean enabled = true;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseCode() { return caseCode; }
    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }
    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public String getFactsJson() { return factsJson; }
    public void setFactsJson(String factsJson) { this.factsJson = factsJson; }
    public String getExpectedDecision() { return expectedDecision; }
    public void setExpectedDecision(String expectedDecision) { this.expectedDecision = expectedDecision; }
    public String getExpectedHitRulesJson() { return expectedHitRulesJson; }
    public void setExpectedHitRulesJson(String expectedHitRulesJson) { this.expectedHitRulesJson = expectedHitRulesJson; }
    public String getExpectedOutputsJson() { return expectedOutputsJson; }
    public void setExpectedOutputsJson(String expectedOutputsJson) { this.expectedOutputsJson = expectedOutputsJson; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
