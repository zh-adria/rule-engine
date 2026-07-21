package com.insurance.ruleengine.infrastructure.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "re_rule_set_step")
public class RuleSetStepEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id", nullable = false)
    private RuleSetEntity ruleSet;
    @Column(name = "step_order", nullable = false)
    private int stepOrder;
    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;
    @Column(name = "rule_version")
    private Integer ruleVersion;
    @Column(nullable = false, length = 16)
    private String mode;
    @Column(name = "stop_on_decline", nullable = false)
    private boolean stopOnDecline;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RuleSetEntity getRuleSet() { return ruleSet; }
    public void setRuleSet(RuleSetEntity ruleSet) { this.ruleSet = ruleSet; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Integer getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(Integer ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isStopOnDecline() { return stopOnDecline; }
    public void setStopOnDecline(boolean stopOnDecline) { this.stopOnDecline = stopOnDecline; }
}
