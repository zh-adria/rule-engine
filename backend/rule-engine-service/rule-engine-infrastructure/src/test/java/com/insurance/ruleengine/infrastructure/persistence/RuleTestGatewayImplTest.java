package com.insurance.ruleengine.infrastructure.persistence;

import com.insurance.ruleengine.domain.model.RuleTestCase;
import com.insurance.ruleengine.domain.model.RuleTestRun;
import com.insurance.ruleengine.domain.model.RuleTestSuite;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTestCaseEntity;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTestRunEntity;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTestSuiteCaseEntity;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTestSuiteEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleTestCaseJpaRepository;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleTestRunJpaRepository;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleTestSuiteCaseJpaRepository;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleTestSuiteJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleTestGatewayImplTest {
    @Mock
    private RuleTestCaseJpaRepository caseRepository;
    @Mock
    private RuleTestSuiteJpaRepository suiteRepository;
    @Mock
    private RuleTestSuiteCaseJpaRepository suiteCaseRepository;
    @Mock
    private RuleTestRunJpaRepository runRepository;

    @InjectMocks
    private RuleTestGatewayImpl gateway;

    @Test
    void saveCaseAndListCasesMapsFields() {
        RuleTestCaseEntity saved = caseEntity("CASE_001", "RULE_001", true);
        when(caseRepository.save(any(RuleTestCaseEntity.class))).thenReturn(saved);
        when(caseRepository.findByRuleCodeAndEnabled("RULE_001", true)).thenReturn(List.of(saved));

        RuleTestCase testCase = new RuleTestCase();
        testCase.setCaseCode("CASE_001");
        testCase.setCaseName("healthy BMI");
        testCase.setRuleCode("RULE_001");
        testCase.setFactsJson("{\"bmi\":22}");
        testCase.setExpectedDecision("ACCEPT");

        RuleTestCase result = gateway.saveCase(testCase);
        List<RuleTestCase> listed = gateway.listCases("RULE_001", true);

        assertEquals("CASE_001", result.getCaseCode());
        assertEquals(1, listed.size());
        assertEquals("RULE_001", listed.get(0).getRuleCode());
    }

    @Test
    void listSuiteCasesReturnsCasesByConfiguredOrder() {
        RuleTestSuiteCaseEntity link1 = suiteCase("SUITE_001", "CASE_002", 2);
        RuleTestSuiteCaseEntity link2 = suiteCase("SUITE_001", "CASE_001", 1);
        when(suiteCaseRepository.findBySuiteCodeOrderByCaseOrderAsc("SUITE_001")).thenReturn(List.of(link2, link1));
        when(caseRepository.findByCaseCode("CASE_001")).thenReturn(Optional.of(caseEntity("CASE_001", "RULE_001", true)));
        when(caseRepository.findByCaseCode("CASE_002")).thenReturn(Optional.of(caseEntity("CASE_002", "RULE_001", true)));

        List<RuleTestCase> cases = gateway.listSuiteCases("SUITE_001");

        assertEquals(List.of("CASE_001", "CASE_002"), cases.stream().map(RuleTestCase::getCaseCode).toList());
    }

    @Test
    void saveRunAndFindRunMapsSummary() {
        RuleTestRunEntity saved = runEntity("run-1", "RULE_001", "FAILED");
        when(runRepository.save(any(RuleTestRunEntity.class))).thenReturn(saved);
        when(runRepository.findByRunId("run-1")).thenReturn(Optional.of(saved));

        RuleTestRun run = RuleTestRun.started("RULE_001", "SUITE_001", null, "tester");
        run.setRunId("run-1");
        run.finish(2, 1, 1, "{\"cases\":[]}");

        RuleTestRun result = gateway.saveRun(run);
        Optional<RuleTestRun> found = gateway.findRun("run-1");

        assertEquals("FAILED", result.getStatus());
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getTotalCases());
    }

    @Test
    void saveSuiteMapsCaseCodes() {
        RuleTestSuiteEntity saved = new RuleTestSuiteEntity();
        saved.setSuiteCode("SUITE_001");
        saved.setSuiteName("underwriting");
        saved.setRuleCode("RULE_001");
        saved.setBusinessLine("LIFE");
        saved.setEnabled(true);
        when(suiteRepository.save(any(RuleTestSuiteEntity.class))).thenReturn(saved);

        RuleTestSuite suite = new RuleTestSuite();
        suite.setSuiteCode("SUITE_001");
        suite.setSuiteName("underwriting");
        suite.setRuleCode("RULE_001");
        suite.setBusinessLine("LIFE");

        RuleTestSuite result = gateway.saveSuite(suite);

        assertEquals("SUITE_001", result.getSuiteCode());
        assertEquals("LIFE", result.getBusinessLine());
    }

    @Test
    void saveSuitePersistsAndReturnsCaseCodes() {
        RuleTestSuiteEntity saved = new RuleTestSuiteEntity();
        saved.setSuiteCode("SUITE_001");
        saved.setSuiteName("underwriting");
        saved.setRuleCode("RULE_001");
        saved.setBusinessLine("LIFE");
        saved.setEnabled(true);
        when(suiteRepository.save(any(RuleTestSuiteEntity.class))).thenReturn(saved);
        when(suiteCaseRepository.findBySuiteCodeOrderByCaseOrderAsc("SUITE_001"))
                .thenReturn(List.of(
                        suiteCase("SUITE_001", "CASE_002", 1),
                        suiteCase("SUITE_001", "CASE_001", 2)));

        RuleTestSuite suite = new RuleTestSuite();
        suite.setSuiteCode("SUITE_001");
        suite.setSuiteName("underwriting");
        suite.setRuleCode("RULE_001");
        suite.setBusinessLine("LIFE");
        suite.setCaseCodes(List.of("CASE_002", "CASE_001"));

        RuleTestSuite result = gateway.saveSuite(suite);

        verify(suiteCaseRepository, times(2)).save(any(RuleTestSuiteCaseEntity.class));
        assertEquals(List.of("CASE_002", "CASE_001"), result.getCaseCodes());
    }

    private RuleTestCaseEntity caseEntity(String caseCode, String ruleCode, boolean enabled) {
        RuleTestCaseEntity entity = new RuleTestCaseEntity();
        entity.setId(1L);
        entity.setCaseCode(caseCode);
        entity.setCaseName(caseCode + " name");
        entity.setRuleCode(ruleCode);
        entity.setScenario("UNIT");
        entity.setFactsJson("{\"age\":30}");
        entity.setExpectedDecision("ACCEPT");
        entity.setExpectedHitRulesJson("[\"HIT\"]");
        entity.setExpectedOutputsJson("{\"riskLevel\":\"LOW\"}");
        entity.setEnabled(enabled);
        entity.setCreatedBy("tester");
        return entity;
    }

    private RuleTestSuiteCaseEntity suiteCase(String suiteCode, String caseCode, int order) {
        RuleTestSuiteCaseEntity entity = new RuleTestSuiteCaseEntity();
        entity.setSuiteCode(suiteCode);
        entity.setCaseCode(caseCode);
        entity.setCaseOrder(order);
        return entity;
    }

    private RuleTestRunEntity runEntity(String runId, String ruleCode, String status) {
        RuleTestRunEntity entity = new RuleTestRunEntity();
        entity.setId(1L);
        entity.setRunId(runId);
        entity.setRuleCode(ruleCode);
        entity.setSuiteCode("SUITE_001");
        entity.setStatus(status);
        entity.setTotalCases(2);
        entity.setPassedCases(1);
        entity.setFailedCases(1);
        entity.setResultJson("{\"cases\":[]}");
        entity.setExecutedBy("tester");
        return entity;
    }
}
