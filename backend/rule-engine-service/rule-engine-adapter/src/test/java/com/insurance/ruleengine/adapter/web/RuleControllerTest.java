package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.infrastructure.drools.DrlConverter;
import com.insurance.ruleengine.client.dto.ArchiveRuleCmd;
import com.insurance.ruleengine.client.dto.CustomFieldDTO;
import com.insurance.ruleengine.client.dto.CreateRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleSetCmd;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleSetCmd;
import com.insurance.ruleengine.client.dto.PublishRuleCmd;
import com.insurance.ruleengine.client.dto.ReviewApprovalCmd;
import com.insurance.ruleengine.client.dto.RollbackRuleCmd;
import com.insurance.ruleengine.client.dto.RuleAuditLogDTO;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionLogDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleSetDTO;
import com.insurance.ruleengine.client.dto.RuleSetExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleVersionDTO;
import com.insurance.ruleengine.client.dto.SubmitApprovalCmd;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleControllerTest {
    @Test
    void routesApprovalEndpointsToFacade() {
        RecordingRuleEngineFacade facade = new RecordingRuleEngineFacade();
        DrlConverter drlConverter = new DrlConverter(new com.fasterxml.jackson.databind.ObjectMapper());
        RuleController controller = new RuleController(facade, drlConverter, new com.fasterxml.jackson.databind.ObjectMapper());

        SubmitApprovalCmd submitCmd = new SubmitApprovalCmd();
        submitCmd.setSubmittedBy("maker");
        ReviewApprovalCmd approveCmd = new ReviewApprovalCmd();
        approveCmd.setReviewedBy("checker");
        ReviewApprovalCmd rejectCmd = new ReviewApprovalCmd();
        rejectCmd.setReviewedBy("checker");

        controller.submitApproval("CI_UW_001", 2, submitCmd);
        assertEquals("submit:CI_UW_001:2:maker", facade.lastCall);

        controller.approveApproval("CI_UW_001", 2, approveCmd);
        assertEquals("approve:CI_UW_001:2:checker", facade.lastCall);

        controller.rejectApproval("CI_UW_001", 2, rejectCmd);
        assertEquals("reject:CI_UW_001:2:checker", facade.lastCall);
    }

    private static class RecordingRuleEngineFacade implements RuleEngineFacade {
        private String lastCall;

        @Override
        public RuleDTO createRule(CreateRuleCmd cmd) { return new RuleDTO(); }

        @Override
        public RuleDTO createVersion(String ruleCode, CreateRuleVersionCmd cmd) { return new RuleDTO(); }

        @Override
        public RuleExecutionResultDTO testRule(String ruleCode, ExecuteRuleCmd cmd) { return new RuleExecutionResultDTO(); }

        @Override
        public RuleDTO submitApproval(String ruleCode, Integer version, SubmitApprovalCmd cmd) {
            lastCall = "submit:" + ruleCode + ":" + version + ":" + cmd.getSubmittedBy();
            return new RuleDTO();
        }

        @Override
        public RuleDTO approveApproval(String ruleCode, Integer version, ReviewApprovalCmd cmd) {
            lastCall = "approve:" + ruleCode + ":" + version + ":" + cmd.getReviewedBy();
            return new RuleDTO();
        }

        @Override
        public RuleDTO rejectApproval(String ruleCode, Integer version, ReviewApprovalCmd cmd) {
            lastCall = "reject:" + ruleCode + ":" + version + ":" + cmd.getReviewedBy();
            return new RuleDTO();
        }

        @Override
        public RuleDTO publish(String ruleCode, PublishRuleCmd cmd) { return new RuleDTO(); }

        @Override
        public RuleDTO rollback(String ruleCode, RollbackRuleCmd cmd) { return new RuleDTO(); }

        @Override
        public RuleExecutionResultDTO execute(ExecuteRuleCmd cmd) { return new RuleExecutionResultDTO(); }

        @Override
        public List<RuleDTO> listRules(String category, String businessLine, String status, String keyword) {
            return Collections.emptyList();
        }

        @Override
        public RuleDTO getRule(String ruleCode) { return new RuleDTO(); }

        @Override
        public List<RuleVersionDTO> listVersions(String ruleCode) { return Collections.emptyList(); }

        @Override
        public List<RuleExecutionLogDTO> listExecutions(String ruleCode) { return Collections.emptyList(); }

        @Override
        public List<RuleAuditLogDTO> listAudits(String ruleCode) { return Collections.emptyList(); }

        @Override
        public RuleDTO archive(String ruleCode, ArchiveRuleCmd cmd) { return new RuleDTO(); }

        @Override
        public RuleVersionDTO getVersion(String ruleCode, Integer version) { return new RuleVersionDTO(); }

        @Override
        public RuleSetDTO createRuleSet(CreateRuleSetCmd cmd) { return new RuleSetDTO(); }

        @Override
        public RuleSetDTO getRuleSet(String setCode) { return new RuleSetDTO(); }

        @Override
        public RuleSetDTO updateRuleSet(String setCode, CreateRuleSetCmd cmd) { return new RuleSetDTO(); }

        @Override
        public void deleteRuleSet(String setCode) { }

        @Override
        public List<RuleSetDTO> listRuleSets() { return Collections.emptyList(); }

        @Override
        public RuleSetExecutionResultDTO executeRuleSet(ExecuteRuleSetCmd cmd) { return new RuleSetExecutionResultDTO(); }

        @Override
        public List<CustomFieldDTO> listCustomFields(String businessLine) { return Collections.emptyList(); }

        @Override
        public List<CustomFieldDTO> listAllCustomFields() { return Collections.emptyList(); }

        @Override
        public CustomFieldDTO createCustomField(CustomFieldDTO field) { return field; }

        @Override
        public void deleteCustomField(Long id) { }

        @Override
        public RuleDTO handleApprovalCallback(String ruleCode, Integer version, String status,
                                              String reviewedBy, String reason) {
            lastCall = "callback:" + ruleCode + ":" + version + ":" + status;
            return new RuleDTO();
        }
    }
}
