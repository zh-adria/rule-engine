package com.insurance.ruleengine.client.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RuleTestSuiteDTO {
    private Long id;
    private String suiteCode;
    private String suiteName;
    private String ruleCode;
    private String businessLine;
    private String description;
    private boolean enabled = true;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> caseCodes = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSuiteCode() { return suiteCode; }
    public void setSuiteCode(String suiteCode) { this.suiteCode = suiteCode; }
    public String getSuiteName() { return suiteName; }
    public void setSuiteName(String suiteName) { this.suiteName = suiteName; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getBusinessLine() { return businessLine; }
    public void setBusinessLine(String businessLine) { this.businessLine = businessLine; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<String> getCaseCodes() { return caseCodes; }
    public void setCaseCodes(List<String> caseCodes) { this.caseCodes = caseCodes; }
}
