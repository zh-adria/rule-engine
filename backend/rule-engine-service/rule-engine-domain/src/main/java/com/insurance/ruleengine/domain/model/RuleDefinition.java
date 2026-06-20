package com.insurance.ruleengine.domain.model;

public class RuleDefinition {
    private Long id;
    private String ruleCode;
    private String ruleName;
    private RuleCategory category;
    private String businessLine;
    private String description;
    private boolean sensitive;
    private String owner;
    private Integer currentVersion;
    private Integer grayVersion;
    private Integer grayPercent;
    private String regulatoryRef;
    private boolean archived;

    public static RuleDefinition create(String ruleCode, String ruleName, RuleCategory category, String businessLine,
                                        String description, boolean sensitive, String owner, String regulatoryRef) {
        RuleDefinition rule = new RuleDefinition();
        rule.ruleCode = ruleCode;
        rule.ruleName = ruleName;
        rule.category = category;
        rule.businessLine = businessLine;
        rule.description = description;
        rule.sensitive = sensitive;
        rule.owner = owner;
        rule.regulatoryRef = regulatoryRef;
        rule.grayPercent = 0;
        return rule;
    }

    public void publish(Integer version, int grayPercent) {
        if (grayPercent < 0 || grayPercent > 100) {
            throw new IllegalArgumentException("grayPercent must be between 0 and 100");
        }
        if (grayPercent > 0 && grayPercent < 100) {
            this.grayVersion = version;
            this.grayPercent = grayPercent;
            return;
        }
        this.currentVersion = version;
        this.grayVersion = null;
        this.grayPercent = 0;
    }

    public void rollback(Integer targetVersion) {
        this.currentVersion = targetVersion;
        this.grayVersion = null;
        this.grayPercent = 0;
    }

    public void archive() {
        this.archived = true;
        this.grayVersion = null;
        this.grayPercent = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public RuleCategory getCategory() { return category; }
    public void setCategory(RuleCategory category) { this.category = category; }
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
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
}

