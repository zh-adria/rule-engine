package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class CreateRuleSetCmd {
    @NotBlank
    private String setCode;
    @NotBlank
    private String setName;
    private String description;
    @NotBlank
    private String owner;
    @NotEmpty
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
