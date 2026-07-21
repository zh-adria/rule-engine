package com.insurance.ruleengine.domain.model;

public enum RuleStatus {
    DRAFT,
    TESTING,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    PUBLISHED,
    GRAY,
    ROLLED_BACK,
    ARCHIVED
}
