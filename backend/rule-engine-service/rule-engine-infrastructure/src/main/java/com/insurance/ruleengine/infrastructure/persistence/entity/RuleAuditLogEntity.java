package com.insurance.ruleengine.infrastructure.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "rule_audit_log", indexes = {
        @Index(name = "idx_rule_audit_rule_code", columnList = "rule_code"),
        @Index(name = "idx_rule_audit_created_at", columnList = "created_at")
})
public class RuleAuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;
    @Column
    private Integer version;
    @Column(nullable = false, length = 64)
    private String action;
    @Column(name = "operator", nullable = false, length = 64)
    private String operator;
    @Column(length = 512)
    private String reason;
    @Column(name = "ip_address", length = 64)
    private String ipAddress;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public void setVersion(Integer version) { this.version = version; }
    public void setAction(String action) { this.action = action; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setReason(String reason) { this.reason = reason; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getRuleCode() { return ruleCode; }
    public Integer getVersion() { return version; }
    public String getAction() { return action; }
    public String getOperator() { return operator; }
    public String getReason() { return reason; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

