package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CreateRuleCmd {
    @NotBlank
    private String ruleCode;
    @NotBlank
    private String ruleName;
    @NotBlank
    private String category;
    @NotBlank
    private String businessLine;
    private String description;
    private boolean sensitive;
    @NotBlank
    private String owner;
    private String regulatoryRef;

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
    public String getRegulatoryRef() { return regulatoryRef; }
    public void setRegulatoryRef(String regulatoryRef) { this.regulatoryRef = regulatoryRef; }
}

