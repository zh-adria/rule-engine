package com.insurance.ruleengine.infrastructure.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "rule_definition", indexes = {
        @Index(name = "idx_rule_definition_code", columnList = "rule_code", unique = true),
        @Index(name = "idx_rule_definition_category", columnList = "category")
})
public class RuleDefinitionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;
    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;
    @Column(nullable = false, length = 32)
    private String category;
    @Column(name = "business_line", nullable = false, length = 64)
    private String businessLine;
    @Column(length = 512)
    private String description;
    @Column(nullable = false)
    private boolean sensitive;
    @Column(nullable = false, length = 64)
    private String owner;
    @Column(name = "current_version")
    private Integer currentVersion;
    @Column(name = "gray_version")
    private Integer grayVersion;
    @Column(name = "gray_percent")
    private Integer grayPercent;
    @Column(name = "regulatory_ref", length = 128)
    private String regulatoryRef;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (grayPercent == null) {
            grayPercent = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBusinessLine() { return businessLine; }
    public void setBusinessLine(String businessLine) { this.businessLine = businessLine; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isSensitive() { return sensitive; }
    public void setSensitive(boolean sensitive) { this.sensitive = sensitive; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public Integer getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }
    public Integer getGrayVersion() { return grayVersion; }
    public void setGrayVersion(Integer grayVersion) { this.grayVersion = grayVersion; }
    public Integer getGrayPercent() { return grayPercent; }
    public void setGrayPercent(Integer grayPercent) { this.grayPercent = grayPercent; }
    public String getRegulatoryRef() { return regulatoryRef; }
    public void setRegulatoryRef(String regulatoryRef) { this.regulatoryRef = regulatoryRef; }
}

