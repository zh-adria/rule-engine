package com.insurance.ruleengine.client.dto;

public class RuleDTO {
    private String ruleCode;
    private String ruleName;
    private String category;
    private String businessLine;
    private String description;
    private String owner;
    private String regulatoryRef;
    private Integer currentVersion;
    private Integer latestVersion;
    private Integer grayVersion;
    private Integer grayPercent;
    private String status;
    private boolean archived;

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
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getRegulatoryRef() { return regulatoryRef; }
    public void setRegulatoryRef(String regulatoryRef) { this.regulatoryRef = regulatoryRef; }
    public Integer getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }
    public Integer getLatestVersion() { return latestVersion; }
    public void setLatestVersion(Integer latestVersion) { this.latestVersion = latestVersion; }
    public Integer getGrayVersion() { return grayVersion; }
    public void setGrayVersion(Integer grayVersion) { this.grayVersion = grayVersion; }
    public Integer getGrayPercent() { return grayPercent; }
    public void setGrayPercent(Integer grayPercent) { this.grayPercent = grayPercent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
}
