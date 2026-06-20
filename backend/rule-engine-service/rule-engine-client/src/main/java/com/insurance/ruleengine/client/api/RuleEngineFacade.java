package com.insurance.ruleengine.client.api;

import com.insurance.ruleengine.client.dto.CreateRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.PublishRuleCmd;
import com.insurance.ruleengine.client.dto.RollbackRuleCmd;
import com.insurance.ruleengine.client.dto.ArchiveRuleCmd;
import com.insurance.ruleengine.client.dto.RuleAuditLogDTO;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionLogDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleVersionDTO;

import java.util.List;

public interface RuleEngineFacade {
    List<RuleDTO> listRules(String category, String businessLine, String status, String keyword);

    RuleDTO getRule(String ruleCode);

    List<RuleVersionDTO> listVersions(String ruleCode);

    List<RuleExecutionLogDTO> listExecutions(String ruleCode);

    List<RuleAuditLogDTO> listAudits(String ruleCode);

    RuleDTO createRule(CreateRuleCmd cmd);

    RuleDTO createVersion(String ruleCode, CreateRuleVersionCmd cmd);

    RuleExecutionResultDTO testRule(String ruleCode, ExecuteRuleCmd cmd);

    RuleDTO publish(String ruleCode, PublishRuleCmd cmd);

    RuleDTO rollback(String ruleCode, RollbackRuleCmd cmd);

    RuleDTO archive(String ruleCode, ArchiveRuleCmd cmd);

    RuleExecutionResultDTO execute(ExecuteRuleCmd cmd);
}

