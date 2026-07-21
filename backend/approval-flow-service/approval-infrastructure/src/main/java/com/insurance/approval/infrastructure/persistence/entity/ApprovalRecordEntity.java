package com.insurance.approval.infrastructure.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "re_approval_record")
public class ApprovalRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String targetType;

    @Column(nullable = false, length = 128)
    private String targetId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 64)
    private String submittedBy;

    @Column(length = 64)
    private String reviewedBy;

    @Column(length = 512)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "current_level")
    private Integer currentLevel;

    @Column(name = "max_level")
    private Integer maxLevel;

    @Column(name = "level_approved_by", length = 64)
    private String levelApprovedBy;

    @Column(name = "level_approved_at")
    private LocalDateTime levelApprovedAt;

    @Column(name = "approval_chain", length = 4096)
    private String approvalChain;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }
    public Integer getMaxLevel() { return maxLevel; }
    public void setMaxLevel(Integer maxLevel) { this.maxLevel = maxLevel; }
    public String getLevelApprovedBy() { return levelApprovedBy; }
    public void setLevelApprovedBy(String levelApprovedBy) { this.levelApprovedBy = levelApprovedBy; }
    public LocalDateTime getLevelApprovedAt() { return levelApprovedAt; }
    public void setLevelApprovedAt(LocalDateTime levelApprovedAt) { this.levelApprovedAt = levelApprovedAt; }
    public String getApprovalChain() { return approvalChain; }
    public void setApprovalChain(String approvalChain) { this.approvalChain = approvalChain; }
}
