package com.insurance.ruleengine.domain.model;

import java.time.LocalDateTime;

public class RuleVersion {
    private Long id;
    private String ruleCode;
    private Integer version;
    private RuleStatus status;
    private String drlContent;
    private String visualModel;
    private String checksum;
    private String createdBy;
    private String approvedBy;
    private LocalDateTime publishedAt;

    public static RuleVersion draft(String ruleCode, Integer version, String drlContent, String visualModel,
                                    String checksum, String createdBy) {
        RuleVersion ruleVersion = new RuleVersion();
        ruleVersion.ruleCode = ruleCode;
        ruleVersion.version = version;
        ruleVersion.drlContent = drlContent;
        ruleVersion.visualModel = visualModel;
        ruleVersion.checksum = checksum;
        ruleVersion.createdBy = createdBy;
        ruleVersion.status = RuleStatus.DRAFT;
        return ruleVersion;
    }

    public void markTesting() {
        if (status == RuleStatus.ARCHIVED) {
            throw new IllegalStateException("archived version can not enter testing");
        }
        status = RuleStatus.TESTING;
    }

    public void publish(String approvedBy, boolean gray) {
        this.approvedBy = approvedBy;
        this.status = gray ? RuleStatus.GRAY : RuleStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void rollback() {
        this.status = RuleStatus.ROLLED_BACK;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public RuleStatus getStatus() { return status; }
    public void setStatus(RuleStatus status) { this.status = status; }
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
}

