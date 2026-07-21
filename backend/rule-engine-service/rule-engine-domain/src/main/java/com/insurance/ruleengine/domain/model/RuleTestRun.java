package com.insurance.ruleengine.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class RuleTestRun {
    private Long id;
    private String runId;
    private String suiteCode;
    private String caseCode;
    private String ruleCode;
    private String status;
    private int totalCases;
    private int passedCases;
    private int failedCases;
    private String resultJson;
    private String executedBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public static RuleTestRun started(String ruleCode, String suiteCode, String caseCode, String executedBy) {
        RuleTestRun run = new RuleTestRun();
        run.runId = UUID.randomUUID().toString();
        run.ruleCode = ruleCode;
        run.suiteCode = suiteCode;
        run.caseCode = caseCode;
        run.executedBy = executedBy;
        run.status = "RUNNING";
        run.startedAt = LocalDateTime.now();
        return run;
    }

    public void finish(int totalCases, int passedCases, int failedCases, String resultJson) {
        this.totalCases = totalCases;
        this.passedCases = passedCases;
        this.failedCases = failedCases;
        this.resultJson = resultJson;
        this.status = failedCases == 0 ? "PASSED" : "FAILED";
        this.finishedAt = LocalDateTime.now();
    }

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
