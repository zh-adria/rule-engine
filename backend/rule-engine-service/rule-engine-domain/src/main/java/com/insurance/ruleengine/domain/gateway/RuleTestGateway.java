package com.insurance.ruleengine.domain.gateway;

import com.insurance.ruleengine.domain.model.RuleTestCase;
import com.insurance.ruleengine.domain.model.RuleTestRun;
import com.insurance.ruleengine.domain.model.RuleTestSuite;

import java.util.List;
import java.util.Optional;

public interface RuleTestGateway {
    RuleTestCase saveCase(RuleTestCase testCase);
    Optional<RuleTestCase> findCase(String caseCode);
    List<RuleTestCase> listCases(String ruleCode, Boolean enabled);
    void deleteCase(String caseCode);

    RuleTestSuite saveSuite(RuleTestSuite suite);
    Optional<RuleTestSuite> findSuite(String suiteCode);
    List<RuleTestSuite> listSuites(String ruleCode, String businessLine, Boolean enabled);
    void deleteSuite(String suiteCode);

    void addCaseToSuite(String suiteCode, String caseCode, int caseOrder);
    void removeCaseFromSuite(String suiteCode, String caseCode);
    List<RuleTestCase> listSuiteCases(String suiteCode);

    RuleTestRun saveRun(RuleTestRun run);
    Optional<RuleTestRun> findRun(String runId);
    List<RuleTestRun> listRuns(String ruleCode, String suiteCode, String caseCode);
}
