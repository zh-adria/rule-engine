package com.insurance.ruleengine.infrastructure.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "re_rule_test_case", indexes = {
        @Index(name = "idx_rule_test_case_code", columnList = "case_code"),
        @Index(name = "idx_rule_test_case_rule", columnList = "rule_code"),
        @Index(name = "idx_rule_test_case_enabled", columnList = "enabled")
})
public class RuleTestCaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "case_code", nullable = false, unique = true, length = 100)
    private String caseCode;
    @Column(name = "case_name", nullable = false, length = 200)
    private String caseName;
    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;
    private Integer version;
    @Column(length = 64)
    private String scenario;
    @Column(name = "facts_json", nullable = false, columnDefinition = "CLOB")
    private String factsJson;
    @Column(name = "expected_decision", length = 32)
    private String expectedDecision;
    @Column(name = "expected_hit_rules_json", columnDefinition = "CLOB")
    private String expectedHitRulesJson;
    @Column(name = "expected_outputs_json", columnDefinition = "CLOB")
    private String expectedOutputsJson;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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
