package com.insurance.ruleengine.infrastructure.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "re_rule_execution_log", indexes = {
        @Index(name = "idx_rule_exec_trace", columnList = "trace_id"),
        @Index(name = "idx_rule_exec_rule_code", columnList = "rule_code"),
        @Index(name = "idx_rule_exec_created_at", columnList = "created_at")
})
public class RuleExecutionLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;
    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;
    @Column(nullable = false)
    private Integer version;
    @Column(nullable = false, length = 64)
    private String scenario;
    @Column(nullable = false, length = 64)
    private String decision;
    @Column(name = "hit_rules", length = 1024)
    private String hitRules;
    @Column(name = "elapsed_ms", nullable = false)
    private long elapsedMs;
    @Lob
    @Column(name = "request_snapshot", columnDefinition = "LONGTEXT")
    private String requestSnapshot;
    @Lob
    @Column(name = "response_snapshot", columnDefinition = "LONGTEXT")
    private String responseSnapshot;
    @Column(name = "operator", length = 64)
    private String operator;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
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
    public String getRequestSnapshot() { return requestSnapshot; }
    public void setRequestSnapshot(String requestSnapshot) { this.requestSnapshot = requestSnapshot; }
    public String getResponseSnapshot() { return responseSnapshot; }
    public void setResponseSnapshot(String responseSnapshot) { this.responseSnapshot = responseSnapshot; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

