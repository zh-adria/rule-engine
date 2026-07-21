package com.insurance.ruleengine.infrastructure.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "re_rule_test_run", indexes = {
        @Index(name = "idx_rule_test_run_id", columnList = "run_id"),
        @Index(name = "idx_rule_test_run_rule", columnList = "rule_code"),
        @Index(name = "idx_rule_test_run_suite", columnList = "suite_code"),
        @Index(name = "idx_rule_test_run_case", columnList = "case_code")
})
public class RuleTestRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false, unique = true, length = 100)
    private String runId;
    @Column(name = "suite_code", length = 100)
    private String suiteCode;
    @Column(name = "case_code", length = 100)
    private String caseCode;
    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "total_cases", nullable = false)
    private int totalCases;
    @Column(name = "passed_cases", nullable = false)
    private int passedCases;
    @Column(name = "failed_cases", nullable = false)
    private int failedCases;
    @Column(name = "result_json", columnDefinition = "CLOB")
    private String resultJson;
    @Column(name = "executed_by", length = 100)
    private String executedBy;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getSuiteCode() { return suiteCode; }
    public void setSuiteCode(String suiteCode) { this.suiteCode = suiteCode; }
    public String getCaseCode() { return caseCode; }
    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalCases() { return totalCases; }
    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }
    public int getPassedCases() { return passedCases; }
    public void setPassedCases(int passedCases) { this.passedCases = passedCases; }
    public int getFailedCases() { return failedCases; }
    public void setFailedCases(int failedCases) { this.failedCases = failedCases; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
