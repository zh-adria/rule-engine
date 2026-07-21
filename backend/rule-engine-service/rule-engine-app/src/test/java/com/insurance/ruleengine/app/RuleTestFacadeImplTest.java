package com.insurance.ruleengine.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.dto.ArchiveRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleCmd;
import com.insurance.ruleengine.client.dto.CreateRuleSetCmd;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.CustomFieldDTO;
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
import com.insurance.ruleengine.client.dto.RuleTestRunDTO;
import com.insurance.ruleengine.client.dto.RuleVersionDTO;
import com.insurance.ruleengine.client.dto.SubmitApprovalCmd;
import com.insurance.ruleengine.domain.gateway.RuleTestGateway;
import com.insurance.ruleengine.domain.model.RuleTestCase;
import com.insurance.ruleengine.domain.model.RuleTestRun;
import com.insurance.ruleengine.domain.model.RuleTestSuite;
import com.insurance.ruleengine.domain.service.RuleTestAssertionService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleTestFacadeImplTest {

    @Test
    void runCasePersistsPassedRunWhenAssertionsMatch() {
        InMemoryRuleTestGateway gateway = new InMemoryRuleTestGateway();
        gateway.cases.put("CASE_PASS", testCase("CASE_PASS", "ACCEPT"));
        FakeRuleEngineFacade ruleEngine = new FakeRuleEngineFacade("ACCEPT");
        RuleTestFacadeImpl facade = new RuleTestFacadeImpl(
                gateway, ruleEngine, new RuleTestAssertionService(), new ObjectMapper());

        RuleTestRunDTO run = facade.runCase("CASE_PASS", "tester");

        assertEquals("PASSED", run.getStatus());
        assertEquals(1, run.getTotalCases());
        assertEquals(1, run.getPassedCases());
        assertEquals("RULE_001", ruleEngine.lastCommand.getRuleCode());
        assertEquals("tester", ruleEngine.lastCommand.getOperator());
        assertTrue(gateway.savedRuns.get(0).getResultJson().contains("\"passed\":true"));
    }

    @Test
    void runCasePersistsFailedRunWhenAssertionsMismatch() {
        InMemoryRuleTestGateway gateway = new InMemoryRuleTestGateway();
        gateway.cases.put("CASE_FAIL", testCase("CASE_FAIL", "ACCEPT"));
        FakeRuleEngineFacade ruleEngine = new FakeRuleEngineFacade("MANUAL_REVIEW");
        RuleTestFacadeImpl facade = new RuleTestFacadeImpl(
                gateway, ruleEngine, new RuleTestAssertionService(), new ObjectMapper());

        RuleTestRunDTO run = facade.runCase("CASE_FAIL", "tester");

        assertEquals("FAILED", run.getStatus());
        assertEquals(1, run.getFailedCases());
        assertTrue(run.getResultJson().contains("decision expected ACCEPT"));
    }

    private RuleTestCase testCase(String caseCode, String expectedDecision) {
        RuleTestCase testCase = new RuleTestCase();
        testCase.setCaseCode(caseCode);
        testCase.setCaseName(caseCode);
        testCase.setRuleCode("RULE_001");
        testCase.setVersion(1);
        testCase.setScenario("UNIT");
        testCase.setFactsJson("{\"age\":30}");
        testCase.setExpectedDecision(expectedDecision);
        testCase.setExpectedOutputsJson("{\"riskLevel\":\"LOW\"}");
        testCase.setEnabled(true);
        return testCase;
    }

    private static class InMemoryRuleTestGateway implements RuleTestGateway {
        private final Map<String, RuleTestCase> cases = new LinkedHashMap<>();
        private final List<RuleTestRun> savedRuns = new ArrayList<>();

        @Override
        public RuleTestCase saveCase(RuleTestCase testCase) { cases.put(testCase.getCaseCode(), testCase); return testCase; }
        @Override
        public Optional<RuleTestCase> findCase(String caseCode) { return Optional.ofNullable(cases.get(caseCode)); }
        @Override
        public List<RuleTestCase> listCases(String ruleCode, Boolean enabled) { return new ArrayList<>(cases.values()); }
        @Override
        public void deleteCase(String caseCode) { cases.remove(caseCode); }
        @Override
        public RuleTestSuite saveSuite(RuleTestSuite suite) { return suite; }
        @Override
        public Optional<RuleTestSuite> findSuite(String suiteCode) { return Optional.empty(); }
        @Override
        public List<RuleTestSuite> listSuites(String ruleCode, String businessLine, Boolean enabled) { return List.of(); }
        @Override
        public void deleteSuite(String suiteCode) { }
        @Override
        public void addCaseToSuite(String suiteCode, String caseCode, int caseOrder) { }
        @Override
        public void removeCaseFromSuite(String suiteCode, String caseCode) { }
        @Override
        public List<RuleTestCase> listSuiteCases(String suiteCode) { return List.of(); }
        @Override
        public RuleTestRun saveRun(RuleTestRun run) { savedRuns.add(run); return run; }
        @Override
        public Optional<RuleTestRun> findRun(String runId) { return savedRuns.stream().filter(r -> r.getRunId().equals(runId)).findFirst(); }
        @Override
        public List<RuleTestRun> listRuns(String ruleCode, String suiteCode, String caseCode) { return savedRuns; }
    }

    private static class FakeRuleEngineFacade implements RuleEngineFacade {
        private final String decision;
        private ExecuteRuleCmd lastCommand;

        private FakeRuleEngineFacade(String decision) {
            this.decision = decision;
        }

        @Override
        public RuleExecutionResultDTO testRule(String ruleCode, ExecuteRuleCmd cmd) {
            lastCommand = cmd;
            RuleExecutionResultDTO dto = new RuleExecutionResultDTO();
            dto.setRuleCode(ruleCode);
            dto.setVersion(cmd.getVersion());
            dto.setDecision(decision);
            dto.setHitRules(List.of("BMI_OK"));
            dto.setOutputs(Map.of("riskLevel", "LOW"));
            return dto;
        }

        @Override public RuleDTO createRule(CreateRuleCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public RuleDTO createVersion(String ruleCode, CreateRuleVersionCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public RuleDTO submitApproval(String ruleCode, Integer version, SubmitApprovalCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public RuleDTO approveApproval(String ruleCode, Integer version, ReviewApprovalCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public RuleDTO rejectApproval(String ruleCode, Integer version, ReviewApprovalCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public RuleDTO handleApprovalCallback(String ruleCode, Integer version, String status, String reviewedBy, String reason) { throw new UnsupportedOperationException(); }
        @Override public RuleDTO publish(String ruleCode, PublishRuleCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public RuleDTO rollback(String ruleCode, RollbackRuleCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public RuleExecutionResultDTO execute(ExecuteRuleCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public List<RuleDTO> listRules(String category, String businessLine, String status, String keyword) { throw new UnsupportedOperationException(); }
        @Override public RuleDTO getRule(String ruleCode) { throw new UnsupportedOperationException(); }
        @Override public List<RuleVersionDTO> listVersions(String ruleCode) { throw new UnsupportedOperationException(); }
        @Override public RuleVersionDTO getVersion(String ruleCode, Integer version) { throw new UnsupportedOperationException(); }
        @Override public List<RuleExecutionLogDTO> listExecutions(String ruleCode) { throw new UnsupportedOperationException(); }
        @Override public List<RuleAuditLogDTO> listAudits(String ruleCode) { throw new UnsupportedOperationException(); }
        @Override public RuleDTO archive(String ruleCode, ArchiveRuleCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public RuleSetDTO createRuleSet(CreateRuleSetCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public RuleSetDTO getRuleSet(String setCode) { throw new UnsupportedOperationException(); }
        @Override public RuleSetDTO updateRuleSet(String setCode, CreateRuleSetCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public void deleteRuleSet(String setCode) { throw new UnsupportedOperationException(); }
        @Override public List<RuleSetDTO> listRuleSets() { throw new UnsupportedOperationException(); }
        @Override public RuleSetExecutionResultDTO executeRuleSet(ExecuteRuleSetCmd cmd) { throw new UnsupportedOperationException(); }
        @Override public List<CustomFieldDTO> listCustomFields(String businessLine) { throw new UnsupportedOperationException(); }
        @Override public List<CustomFieldDTO> listAllCustomFields() { throw new UnsupportedOperationException(); }
        @Override public CustomFieldDTO createCustomField(CustomFieldDTO field) { throw new UnsupportedOperationException(); }
        @Override public void deleteCustomField(Long id) { throw new UnsupportedOperationException(); }
    }
}
