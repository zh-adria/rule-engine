package com.insurance.ruleengine.client.dto;

public class RuleTemplateDTO {
    private Long id;
    private String templateCode;
    private String templateName;
    private String category;
    private String businessLine;
    private String description;
    private String drlTemplate;
    private String visualTemplate;
    private boolean sensitive;
    private String owner;
    private Integer sortOrder;

    public Long getId() { return id; }
    public String getTemplateCode() { return templateCode; }
    public String getTemplateName() { return templateName; }
    public String getCategory() { return category; }
    public String getBusinessLine() { return businessLine; }
    public String getDescription() { return description; }
    public String getDrlTemplate() { return drlTemplate; }
    public String getVisualTemplate() { return visualTemplate; }
    public boolean isSensitive() { return sensitive; }
    public String getOwner() { return owner; }
    public Integer getSortOrder() { return sortOrder; }

    public void setId(Long id) { this.id = id; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public void setCategory(String category) { this.category = category; }
    public void setBusinessLine(String businessLine) { this.businessLine = businessLine; }
    public void setDescription(String description) { this.description = description; }
    public void setDrlTemplate(String drlTemplate) { this.drlTemplate = drlTemplate; }
    public void setVisualTemplate(String visualTemplate) { this.visualTemplate = visualTemplate; }
    public void setSensitive(boolean sensitive) { this.sensitive = sensitive; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
