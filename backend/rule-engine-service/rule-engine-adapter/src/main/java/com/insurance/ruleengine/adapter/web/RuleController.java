package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.dto.CreateRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.PublishRuleCmd;
import com.insurance.ruleengine.client.dto.RollbackRuleCmd;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {
    private final RuleEngineFacade ruleEngineFacade;

    public RuleController(RuleEngineFacade ruleEngineFacade) {
        this.ruleEngineFacade = ruleEngineFacade;
    }

    @PostMapping
    public RuleDTO createRule(@Valid @RequestBody CreateRuleCmd cmd) {
        return ruleEngineFacade.createRule(cmd);
    }

    @PostMapping("/{ruleCode}/versions")
    public RuleDTO createVersion(@PathVariable String ruleCode, @Valid @RequestBody CreateRuleVersionCmd cmd) {
        return ruleEngineFacade.createVersion(ruleCode, cmd);
    }

    @PostMapping("/{ruleCode}/test")
    public RuleExecutionResultDTO test(@PathVariable String ruleCode, @Valid @RequestBody ExecuteRuleCmd cmd) {
        cmd.setRuleCode(ruleCode);
        return ruleEngineFacade.testRule(ruleCode, cmd);
    }

    @PostMapping("/{ruleCode}/publish")
    public RuleDTO publish(@PathVariable String ruleCode, @Valid @RequestBody PublishRuleCmd cmd) {
        return ruleEngineFacade.publish(ruleCode, cmd);
    }

    @PostMapping("/{ruleCode}/rollback")
    public RuleDTO rollback(@PathVariable String ruleCode, @Valid @RequestBody RollbackRuleCmd cmd) {
        return ruleEngineFacade.rollback(ruleCode, cmd);
    }

    @PostMapping("/execute")
    public RuleExecutionResultDTO execute(@Valid @RequestBody ExecuteRuleCmd cmd) {
        return ruleEngineFacade.execute(cmd);
    }
}

