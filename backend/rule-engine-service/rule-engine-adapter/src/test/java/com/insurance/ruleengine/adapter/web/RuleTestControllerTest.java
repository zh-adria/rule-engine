package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.RuleTestFacade;
import com.insurance.ruleengine.client.dto.RuleTestCaseDTO;
import com.insurance.ruleengine.client.dto.RuleTestRunDTO;
import com.insurance.ruleengine.client.dto.RuleTestSuiteDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleTestControllerTest {

    @Test
    void routesCaseCrudAndRunToFacade() {
        RecordingRuleTestFacade facade = new RecordingRuleTestFacade();
        RuleTestController controller = new RuleTestController(facade);
        RuleTestCaseDTO request = new RuleTestCaseDTO();
        request.setCaseCode("CASE_001");
        request.setRuleCode("RULE_001");

        ResponseEntity<RuleTestCaseDTO> created = controller.createCase(request);
        List<RuleTestCaseDTO> listed = controller.listCases("RULE_001", true);
        RuleTestRunDTO run = controller.runCase("CASE_001", "tester");

        assertEquals(201, created.getStatusCodeValue());
        assertEquals("createCase:CASE_001", facade.calls.get(0));
        assertEquals("listCases:RULE_001:true", facade.calls.get(1));
        assertEquals(1, listed.size());
        assertEquals("runCase:CASE_001:tester", facade.calls.get(2));
        assertEquals("PASSED", run.getStatus());
    }

    private static class RecordingRuleTestFacade implements RuleTestFacade {
        private final List<String> calls = new ArrayList<>();

        @Override
        public RuleTestCaseDTO createCase(RuleTestCaseDTO testCase) {
            calls.add("createCase:" + testCase.getCaseCode());
            return testCase;
        }

        @Override
        public RuleTestCaseDTO updateCase(String caseCode, RuleTestCaseDTO testCase) {
            calls.add("updateCase:" + caseCode);
            return testCase;
        }

        @Override
        public Optional<RuleTestCaseDTO> findCase(String caseCode) {
            RuleTestCaseDTO dto = new RuleTestCaseDTO();
            dto.setCaseCode(caseCode);
            return Optional.of(dto);
        }

        @Override
        public List<RuleTestCaseDTO> listCases(String ruleCode, Boolean enabled) {
            calls.add("listCases:" + ruleCode + ":" + enabled);
            RuleTestCaseDTO dto = new RuleTestCaseDTO();
            dto.setCaseCode("CASE_001");
            dto.setRuleCode(ruleCode);
            return List.of(dto);
        }

        @Override
        public void deleteCase(String caseCode) {
            calls.add("deleteCase:" + caseCode);
        }

        @Override
        public RuleTestSuiteDTO createSuite(RuleTestSuiteDTO suite) { return suite; }
        @Override
        public RuleTestSuiteDTO updateSuite(String suiteCode, RuleTestSuiteDTO suite) { return suite; }
        @Override
        public Optional<RuleTestSuiteDTO> findSuite(String suiteCode) { return Optional.empty(); }
        @Override
        public List<RuleTestSuiteDTO> listSuites(String ruleCode, String businessLine, Boolean enabled) { return List.of(); }
        @Override
        public void deleteSuite(String suiteCode) { }
        @Override
        public void addCaseToSuite(String suiteCode, String caseCode, Integer caseOrder) { }
        @Override
        public void removeCaseFromSuite(String suiteCode, String caseCode) { }

        @Override
        public RuleTestRunDTO runCase(String caseCode, String executedBy) {
            calls.add("runCase:" + caseCode + ":" + executedBy);
            RuleTestRunDTO dto = new RuleTestRunDTO();
            dto.setCaseCode(caseCode);
            dto.setStatus("PASSED");
            return dto;
        }

        @Override
        public RuleTestRunDTO runSuite(String suiteCode, String executedBy) {
            RuleTestRunDTO dto = new RuleTestRunDTO();
            dto.setSuiteCode(suiteCode);
            return dto;
        }

        @Override
        public Optional<RuleTestRunDTO> findRun(String runId) { return Optional.empty(); }
        @Override
        public List<RuleTestRunDTO> listRuns(String ruleCode, String suiteCode, String caseCode) { return List.of(); }
    }
}
