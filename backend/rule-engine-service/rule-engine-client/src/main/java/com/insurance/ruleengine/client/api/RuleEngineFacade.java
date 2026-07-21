package com.insurance.ruleengine.client.api;

import com.insurance.ruleengine.client.dto.ArchiveRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleSetCmd;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleSetCmd;
import com.insurance.ruleengine.client.dto.PublishRuleCmd;
import com.insurance.ruleengine.client.dto.ReviewApprovalCmd;
import com.insurance.ruleengine.client.dto.RollbackRuleCmd;
import com.insurance.ruleengine.client.dto.CustomFieldDTO;
import com.insurance.ruleengine.client.dto.RuleAuditLogDTO;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionLogDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleSetDTO;
import com.insurance.ruleengine.client.dto.RuleSetExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleVersionDTO;
import com.insurance.ruleengine.client.dto.SubmitApprovalCmd;

import java.util.List;

public interface RuleEngineFacade {
    RuleDTO createRule(CreateRuleCmd cmd);

    RuleDTO createVersion(String ruleCode, CreateRuleVersionCmd cmd);

    RuleExecutionResultDTO testRule(String ruleCode, ExecuteRuleCmd cmd);

    RuleDTO submitApproval(String ruleCode, Integer version, SubmitApprovalCmd cmd);

    RuleDTO approveApproval(String ruleCode, Integer version, ReviewApprovalCmd cmd);

    RuleDTO rejectApproval(String ruleCode, Integer version, ReviewApprovalCmd cmd);

    /**
     * Approval callback - called by approval-flow-service after approval/rejection decision.
     * Transitions rule version from PENDING_APPROVAL to APPROVED or REJECTED.
     *
     * @param ruleCode  rule code
     * @param version   version number
     * @param status    "APPROVED" or "REJECTED"
     * @param reviewedBy reviewer operator
     * @param reason    review reason
     * @return updated rule DTO
     * @throws IllegalStateException if version is not in PENDING_APPROVAL status
     */
    RuleDTO handleApprovalCallback(String ruleCode, Integer version, String status, String reviewedBy, String reason);

    RuleDTO publish(String ruleCode, PublishRuleCmd cmd);

    RuleDTO rollback(String ruleCode, RollbackRuleCmd cmd);

    RuleExecutionResultDTO execute(ExecuteRuleCmd cmd);

    List<RuleDTO> listRules(String category, String businessLine, String status, String keyword);

    RuleDTO getRule(String ruleCode);

    List<RuleVersionDTO> listVersions(String ruleCode);

    RuleVersionDTO getVersion(String ruleCode, Integer version);

    List<RuleExecutionLogDTO> listExecutions(String ruleCode);

    List<RuleAuditLogDTO> listAudits(String ruleCode);

    RuleDTO archive(String ruleCode, ArchiveRuleCmd cmd);

    // Rule Set operations
    RuleSetDTO createRuleSet(CreateRuleSetCmd cmd);

    RuleSetDTO getRuleSet(String setCode);

    RuleSetDTO updateRuleSet(String setCode, CreateRuleSetCmd cmd);

    void deleteRuleSet(String setCode);

    List<RuleSetDTO> listRuleSets();

    RuleSetExecutionResultDTO executeRuleSet(ExecuteRuleSetCmd cmd);

    // Custom fields
    List<CustomFieldDTO> listCustomFields(String businessLine);

    List<CustomFieldDTO> listAllCustomFields();

    CustomFieldDTO createCustomField(CustomFieldDTO field);

    void deleteCustomField(Long id);
}
