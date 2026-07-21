package com.insurance.ruleengine.client.api;

import com.insurance.ruleengine.client.dto.RuleTestCaseDTO;
import com.insurance.ruleengine.client.dto.RuleTestRunDTO;
import com.insurance.ruleengine.client.dto.RuleTestSuiteDTO;

import java.util.List;
import java.util.Optional;

public interface RuleTestFacade {
    RuleTestCaseDTO createCase(RuleTestCaseDTO testCase);
    RuleTestCaseDTO updateCase(String caseCode, RuleTestCaseDTO testCase);
    Optional<RuleTestCaseDTO> findCase(String caseCode);
    List<RuleTestCaseDTO> listCases(String ruleCode, Boolean enabled);
    void deleteCase(String caseCode);

    RuleTestSuiteDTO createSuite(RuleTestSuiteDTO suite);
    RuleTestSuiteDTO updateSuite(String suiteCode, RuleTestSuiteDTO suite);
    Optional<RuleTestSuiteDTO> findSuite(String suiteCode);
    List<RuleTestSuiteDTO> listSuites(String ruleCode, String businessLine, Boolean enabled);
    void deleteSuite(String suiteCode);
    void addCaseToSuite(String suiteCode, String caseCode, Integer caseOrder);
    void removeCaseFromSuite(String suiteCode, String caseCode);

    RuleTestRunDTO runCase(String caseCode, String executedBy);
    RuleTestRunDTO runSuite(String suiteCode, String executedBy);
    Optional<RuleTestRunDTO> findRun(String runId);
    List<RuleTestRunDTO> listRuns(String ruleCode, String suiteCode, String caseCode);
}
