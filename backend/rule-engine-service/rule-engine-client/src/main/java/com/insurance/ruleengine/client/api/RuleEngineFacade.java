package com.insurance.ruleengine.client.api;

import com.insurance.ruleengine.client.dto.CreateRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.PublishRuleCmd;
import com.insurance.ruleengine.client.dto.RollbackRuleCmd;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;

public interface RuleEngineFacade {
    RuleDTO createRule(CreateRuleCmd cmd);

    RuleDTO createVersion(String ruleCode, CreateRuleVersionCmd cmd);

    RuleExecutionResultDTO testRule(String ruleCode, ExecuteRuleCmd cmd);

    RuleDTO publish(String ruleCode, PublishRuleCmd cmd);

    RuleDTO rollback(String ruleCode, RollbackRuleCmd cmd);

    RuleExecutionResultDTO execute(ExecuteRuleCmd cmd);
}

