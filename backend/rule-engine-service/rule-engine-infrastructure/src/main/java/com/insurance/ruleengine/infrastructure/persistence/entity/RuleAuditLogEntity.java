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
@Table(name = "re_rule_audit_log", indexes = {
        @Index(name = "idx_rule_audit_rule_code", columnList = "rule_code"),
        @Index(name = "idx_rule_audit_created_at", columnList = "created_at"),
        @Index(name = "idx_rule_audit_hash_chain", columnList = "rule_code, audit_hash")
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
    @Column(name = "before_json", columnDefinition = "CLOB")
    private String beforeJson;
    @Column(name = "after_json", columnDefinition = "CLOB")
    private String afterJson;
    @Column(name = "audit_hash", length = 64)
    private String auditHash;
    @Column(name = "previous_hash", length = 64)
    private String previousHash;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String beforeJson) { this.beforeJson = beforeJson; }
    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String afterJson) { this.afterJson = afterJson; }
    public String getAuditHash() { return auditHash; }
    public void setAuditHash(String auditHash) { this.auditHash = auditHash; }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
