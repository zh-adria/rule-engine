package com.insurance.ruleengine.client.dto;

import java.util.List;

public class RuleSetDTO {
    private String setCode;
    private String setName;
    private String description;
    private String owner;
    private List<RuleSetStepDTO> steps;

    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }
    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public List<RuleSetStepDTO> getSteps() { return steps; }
    public void setSteps(List<RuleSetStepDTO> steps) { this.steps = steps; }
}
