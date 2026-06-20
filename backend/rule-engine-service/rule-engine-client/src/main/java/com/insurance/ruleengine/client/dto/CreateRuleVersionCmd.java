package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.NotBlank;

public class CreateRuleVersionCmd {
    @NotBlank
    private String drlContent;
    private String visualModel;
    @NotBlank
    private String createdBy;

    public String getDrlContent() { return drlContent; }
    public void setDrlContent(String drlContent) { this.drlContent = drlContent; }
    public String getVisualModel() { return visualModel; }
    public void setVisualModel(String visualModel) { this.visualModel = visualModel; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

