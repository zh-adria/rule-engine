package com.insurance.ruleengine.infrastructure.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "re_rule_test_suite_case", indexes = {
        @Index(name = "idx_rule_test_suite_case_suite", columnList = "suite_code"),
        @Index(name = "idx_rule_test_suite_case_case", columnList = "case_code")
})
public class RuleTestSuiteCaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "suite_code", nullable = false, length = 100)
    private String suiteCode;
    @Column(name = "case_code", nullable = false, length = 100)
    private String caseCode;
    @Column(name = "case_order", nullable = false)
    private int caseOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSuiteCode() { return suiteCode; }
    public void setSuiteCode(String suiteCode) { this.suiteCode = suiteCode; }
    public String getCaseCode() { return caseCode; }
    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }
    public int getCaseOrder() { return caseOrder; }
    public void setCaseOrder(int caseOrder) { this.caseOrder = caseOrder; }
}
