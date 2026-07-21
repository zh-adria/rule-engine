package com.insurance.ruleengine.domain.model;

import java.util.ArrayList;
import java.util.List;

public class RuleSet {
    private Long id;
    private String setCode;
    private String setName;
    private String description;
    private String owner;
    private List<RuleSetStep> steps = new ArrayList<>();

    public static RuleSet create(String setCode, String setName, String description, String owner) {
        RuleSet ruleSet = new RuleSet();
        ruleSet.setCode = setCode;
        ruleSet.setName = setName;
        ruleSet.description = description;
        ruleSet.owner = owner;
        return ruleSet;
    }

    public void addStep(RuleSetStep step) {
        steps.add(step);
    }

    public void removeStep(int stepOrder) {
        steps.removeIf(s -> s.getStepOrder() == stepOrder);
    }

    public void reorderSteps(List<RuleSetStep> newSteps) {
        this.steps = new ArrayList<>(newSteps);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }
    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public List<RuleSetStep> getSteps() { return steps; }
    public void setSteps(List<RuleSetStep> steps) { this.steps = steps; }
}
