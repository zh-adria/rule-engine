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
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "re_rule_version", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rule_version", columnNames = {"rule_code", "version"})
}, indexes = {
        @Index(name = "idx_rule_version_status", columnList = "status"),
        @Index(name = "idx_rule_version_rule_code", columnList = "rule_code")
})
public class RuleVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;
    @Column(nullable = false)
    private Integer version;
    @Column(nullable = false, length = 32)
    private String status;
    @Lob
    @Column(name = "drl_content", nullable = false, columnDefinition = "LONGTEXT")
    private String drlContent;
    @Lob
    @Column(name = "visual_model", columnDefinition = "LONGTEXT")
    private String visualModel;
    @Column(nullable = false, length = 64)
    private String checksum;
    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;
    @Column(name = "approved_by", length = 64)
    private String approvedBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDrlContent() { return drlContent; }
    public void setDrlContent(String drlContent) { this.drlContent = drlContent; }
    public String getVisualModel() { return visualModel; }
    public void setVisualModel(String visualModel) { this.visualModel = visualModel; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDateTime effectiveTo) { this.effectiveTo = effectiveTo; }
}

