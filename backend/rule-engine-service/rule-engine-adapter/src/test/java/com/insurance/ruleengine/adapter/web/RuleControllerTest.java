package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.dto.ArchiveRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.PublishRuleCmd;
import com.insurance.ruleengine.client.dto.RollbackRuleCmd;
import com.insurance.ruleengine.client.dto.RuleAuditLogDTO;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionLogDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleVersionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RuleControllerTest {
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RuleController(new StubRuleEngineFacade())).build();

    @Test
    void listRulesDelegatesToFacade() throws Exception {
        mockMvc.perform(get("/api/v1/rules").param("keyword", "CI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ruleCode").value("CI_UW_001"));
    }

    @Test
    void archiveDelegatesToFacade() throws Exception {
        mockMvc.perform(post("/api/v1/rules/CI_UW_001/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operator\":\"admin\",\"reason\":\"retired\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    private static class StubRuleEngineFacade implements RuleEngineFacade {
        @Override
        public List<RuleDTO> listRules(String category, String businessLine, String status, String keyword) {
            RuleDTO dto = new RuleDTO();
            dto.setRuleCode("CI_UW_001");
            dto.setRuleName("name");
            return List.of(dto);
        }

        @Override
        public RuleDTO getRule(String ruleCode) { return new RuleDTO(); }

        @Override
        public List<RuleVersionDTO> listVersions(String ruleCode) { return List.of(); }

        @Override
        public List<RuleExecutionLogDTO> listExecutions(String ruleCode) { return List.of(); }

        @Override
        public List<RuleAuditLogDTO> listAudits(String ruleCode) { return List.of(); }

        @Override
        public RuleDTO createRule(CreateRuleCmd cmd) { return new RuleDTO(); }

        @Override
        public RuleDTO createVersion(String ruleCode, CreateRuleVersionCmd cmd) { return new RuleDTO(); }

        @Override
        public RuleExecutionResultDTO testRule(String ruleCode, ExecuteRuleCmd cmd) { return new RuleExecutionResultDTO(); }

        @Override
        public RuleDTO publish(String ruleCode, PublishRuleCmd cmd) { return new RuleDTO(); }

        @Override
        public RuleDTO rollback(String ruleCode, RollbackRuleCmd cmd) { return new RuleDTO(); }

        @Override
        public RuleDTO archive(String ruleCode, ArchiveRuleCmd cmd) {
            RuleDTO dto = new RuleDTO();
            dto.setRuleCode(ruleCode);
            dto.setArchived(true);
            return dto;
        }

        @Override
        public RuleExecutionResultDTO execute(ExecuteRuleCmd cmd) { return new RuleExecutionResultDTO(); }
    }
}
