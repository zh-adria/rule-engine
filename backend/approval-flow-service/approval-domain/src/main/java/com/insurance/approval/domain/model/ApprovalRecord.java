package com.insurance.approval.domain.model;

import java.time.LocalDateTime;

public class ApprovalRecord {

    private Long id;
    private String targetType;
    private String targetId;
    private ApprovalStatus status;
    private String submittedBy;
    private String reviewedBy;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // P3-7: multi-level approval chain
    private int currentLevel;
    private int maxLevel;
    private String levelApprovedBy;
    private LocalDateTime levelApprovedAt;
    private String approvalChain; // JSON: [{"level":1,"approver":"user1","action":"APPROVE","reason":"...","timestamp":"..."}]

    private ApprovalRecord() {}

    public static ApprovalRecord create(String targetType, String targetId, String submittedBy, String reason) {
        ApprovalRecord record = new ApprovalRecord();
        record.targetType = targetType;
        record.targetId = targetId;
        record.status = ApprovalStatus.PENDING;
        record.submittedBy = submittedBy;
        record.reason = reason;
        record.createdAt = LocalDateTime.now();
        record.updatedAt = LocalDateTime.now();
        return record;
    }

    public void approve(String reviewedBy, String reason) {
        if (this.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("只能审批处于待审批状态的记录");
        }
        this.status = ApprovalStatus.APPROVED;
        this.reviewedBy = reviewedBy;
        this.reason = reason;
        this.approvalChain = appendChainEntry(this.approvalChain, 1, reviewedBy, "APPROVE", reason);
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String reviewedBy, String reason) {
        if (this.status != ApprovalStatus.PENDING && this.status != ApprovalStatus.LEVEL_APPROVED) {
            throw new IllegalStateException("只能驳回处于待审批或逐级审批中的记录");
        }
        this.status = ApprovalStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.reason = reason;
        this.approvalChain = appendChainEntry(this.approvalChain, this.currentLevel, reviewedBy, "REJECT", reason);
        this.updatedAt = LocalDateTime.now();
    }

    // P3-7: multi-level approval

    public void initLevelApproval(int maxLevel) {
        this.currentLevel = 1;
        this.maxLevel = maxLevel;
        this.approvalChain = "[]";
        this.status = ApprovalStatus.PENDING;
    }

    public void approveLevel(String reviewer, String reason) {
        if (this.status != ApprovalStatus.PENDING && this.status != ApprovalStatus.LEVEL_APPROVED) {
            throw new IllegalStateException("只能审批处于待审批或逐级审批中的记录");
        }
        int level = this.currentLevel;
        this.levelApprovedBy = reviewer;
        this.levelApprovedAt = LocalDateTime.now();
        this.reason = reason;
        this.currentLevel++;
        if (this.currentLevel > this.maxLevel) {
            this.status = ApprovalStatus.APPROVED;
            this.reviewedBy = reviewer;
        } else {
            this.status = ApprovalStatus.LEVEL_APPROVED;
        }
        this.approvalChain = appendChainEntry(this.approvalChain, level, reviewer, "APPROVE", reason);
        this.updatedAt = LocalDateTime.now();
    }

    private static String appendChainEntry(String chain, int level, String approver, String action, String reason) {
        String entry = String.format("{\"level\":%d,\"approver\":\"%s\",\"action\":\"%s\",\"reason\":\"%s\",\"timestamp\":\"%s\"}",
                level, escapeJson(approver), action, escapeJson(reason), LocalDateTime.now().toString());
        if (chain == null || chain.isBlank() || chain.equals("[]")) {
            return "[" + entry + "]";
        }
        // Remove trailing ] and append
        return chain.substring(0, chain.length() - 1) + "," + entry + "]";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }

    public boolean isChainComplete() {
        return currentLevel > maxLevel;
    }

    public boolean isLevelApproved() {
        return status == ApprovalStatus.LEVEL_APPROVED;
    }

    // getters/setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }
    public String getSubmittedBy() { return submittedBy; }
    public String getReviewedBy() { return reviewedBy; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    public int getMaxLevel() { return maxLevel; }
    public void setMaxLevel(int maxLevel) { this.maxLevel = maxLevel; }
    public String getLevelApprovedBy() { return levelApprovedBy; }
    public void setLevelApprovedBy(String levelApprovedBy) { this.levelApprovedBy = levelApprovedBy; }
    public LocalDateTime getLevelApprovedAt() { return levelApprovedAt; }
    public void setLevelApprovedAt(LocalDateTime levelApprovedAt) { this.levelApprovedAt = levelApprovedAt; }
    public String getApprovalChain() { return approvalChain; }
    public void setApprovalChain(String approvalChain) { this.approvalChain = approvalChain; }
}
