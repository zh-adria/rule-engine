package com.insurance.ruleengine.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.api.RuleTestFacade;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleTestCaseDTO;
import com.insurance.ruleengine.client.dto.RuleTestRunDTO;
import com.insurance.ruleengine.client.dto.RuleTestSuiteDTO;
import com.insurance.ruleengine.domain.gateway.RuleTestGateway;
import com.insurance.ruleengine.domain.model.DecisionType;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleTestCase;
import com.insurance.ruleengine.domain.model.RuleTestRun;
import com.insurance.ruleengine.domain.model.RuleTestSuite;
import com.insurance.ruleengine.domain.service.RuleTestAssertionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RuleTestFacadeImpl implements RuleTestFacade {
    private final RuleTestGateway ruleTestGateway;
    private final RuleEngineFacade ruleEngineFacade;
    private final RuleTestAssertionService assertionService;
    private final ObjectMapper objectMapper;

    public RuleTestFacadeImpl(RuleTestGateway ruleTestGateway,
                              RuleEngineFacade ruleEngineFacade,
                              RuleTestAssertionService assertionService,
                              ObjectMapper objectMapper) {
        this.ruleTestGateway = ruleTestGateway;
        this.ruleEngineFacade = ruleEngineFacade;
        this.assertionService = assertionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public RuleTestCaseDTO createCase(RuleTestCaseDTO testCase) {
        validateJson(testCase);
        return toDTO(ruleTestGateway.saveCase(toDomain(testCase)));
    }

    @Override
    public RuleTestCaseDTO updateCase(String caseCode, RuleTestCaseDTO testCase) {
        validateJson(testCase);
        RuleTestCase existing = ruleTestGateway.findCase(caseCode)
                .orElseThrow(() -> new IllegalArgumentException("test case not found: " + caseCode));
        RuleTestCase updated = toDomain(testCase);
        updated.setId(existing.getId());
        updated.setCaseCode(caseCode);
        return toDTO(ruleTestGateway.saveCase(updated));
    }

    @Override
    public Optional<RuleTestCaseDTO> findCase(String caseCode) {
        return ruleTestGateway.findCase(caseCode).map(this::toDTO);
    }

    @Override
    public List<RuleTestCaseDTO> listCases(String ruleCode, Boolean enabled) {
        return ruleTestGateway.listCases(ruleCode, enabled).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteCase(String caseCode) {
        ruleTestGateway.deleteCase(caseCode);
    }

    @Override
    public RuleTestSuiteDTO createSuite(RuleTestSuiteDTO suite) {
        return toDTO(ruleTestGateway.saveSuite(toDomain(suite)));
    }

    @Override
    public RuleTestSuiteDTO updateSuite(String suiteCode, RuleTestSuiteDTO suite) {
        RuleTestSuite existing = ruleTestGateway.findSuite(suiteCode)
                .orElseThrow(() -> new IllegalArgumentException("test suite not found: " + suiteCode));
        RuleTestSuite updated = toDomain(suite);
        updated.setId(existing.getId());
        updated.setSuiteCode(suiteCode);
        return toDTO(ruleTestGateway.saveSuite(updated));
    }

    @Override
    public Optional<RuleTestSuiteDTO> findSuite(String suiteCode) {
        return ruleTestGateway.findSuite(suiteCode).map(this::toDTO);
    }

    @Override
    public List<RuleTestSuiteDTO> listSuites(String ruleCode, String businessLine, Boolean enabled) {
        return ruleTestGateway.listSuites(ruleCode, businessLine, enabled).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteSuite(String suiteCode) {
        ruleTestGateway.deleteSuite(suiteCode);
    }

    @Override
    public void addCaseToSuite(String suiteCode, String caseCode, Integer caseOrder) {
        ruleTestGateway.addCaseToSuite(suiteCode, caseCode, caseOrder == null ? 0 : caseOrder);
    }

    @Override
    public void removeCaseFromSuite(String suiteCode, String caseCode) {
        ruleTestGateway.removeCaseFromSuite(suiteCode, caseCode);
    }

    @Override
    @Transactional
    public RuleTestRunDTO runCase(String caseCode, String executedBy) {
        RuleTestCase testCase = ruleTestGateway.findCase(caseCode)
                .orElseThrow(() -> new IllegalArgumentException("test case not found: " + caseCode));
        RuleTestRun run = RuleTestRun.started(testCase.getRuleCode(), null, caseCode, executedBy);
        CaseResult result = executeCase(testCase, executedBy);
        finishRun(run, List.of(result));
        return toDTO(ruleTestGateway.saveRun(run));
    }

    @Override
    @Transactional
    public RuleTestRunDTO runSuite(String suiteCode, String executedBy) {
        List<RuleTestCase> cases = ruleTestGateway.listSuiteCases(suiteCode).stream()
                .filter(RuleTestCase::isEnabled)
                .collect(Collectors.toList());
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("test suite has no enabled cases: " + suiteCode);
        }
        RuleTestRun run = RuleTestRun.started(cases.get(0).getRuleCode(), suiteCode, null, executedBy);
        List<CaseResult> results = cases.stream()
                .map(testCase -> executeCase(testCase, executedBy))
                .collect(Collectors.toList());
        finishRun(run, results);
        return toDTO(ruleTestGateway.saveRun(run));
    }

    @Override
    public Optional<RuleTestRunDTO> findRun(String runId) {
        return ruleTestGateway.findRun(runId).map(this::toDTO);
    }

    @Override
    public List<RuleTestRunDTO> listRuns(String ruleCode, String suiteCode, String caseCode) {
        return ruleTestGateway.listRuns(ruleCode, suiteCode, caseCode).stream().map(this::toDTO).collect(Collectors.toList());
    }

    private CaseResult executeCase(RuleTestCase testCase, String executedBy) {
        ExecuteRuleCmd cmd = new ExecuteRuleCmd();
        cmd.setRuleCode(testCase.getRuleCode());
        cmd.setVersion(testCase.getVersion());
        cmd.setScenario(testCase.getScenario() == null ? "RULE_TEST" : testCase.getScenario());
        cmd.setFacts(readMap(testCase.getFactsJson(), "factsJson"));
        cmd.setOperator(executedBy);
        RuleExecutionResultDTO actual = ruleEngineFacade.testRule(testCase.getRuleCode(), cmd);
        List<String> errors = assertionService.assertResult(testCase, toExecutionResult(actual));
        return new CaseResult(testCase.getCaseCode(), errors.isEmpty(), errors, actual);
    }

    private void finishRun(RuleTestRun run, List<CaseResult> results) {
        int passed = (int) results.stream().filter(CaseResult::passed).count();
        int failed = results.size() - passed;
        run.finish(results.size(), passed, failed, writeJson(Map.of("cases", results)));
    }

    private ExecutionResult toExecutionResult(RuleExecutionResultDTO dto) {
        ExecutionResult result = new ExecutionResult();
        result.setTraceId(dto.getTraceId());
        result.setRuleCode(dto.getRuleCode());
        result.setVersion(dto.getVersion());
        result.setDecision(dto.getDecision() == null ? null : DecisionType.valueOf(dto.getDecision()));
        result.setHitRules(dto.getHitRules());
        result.setOutputs(dto.getOutputs());
        result.setElapsedMs(dto.getElapsedMs());
        return result;
    }

    private Map<String, Object> readMap(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " is invalid JSON", e);
        }
    }

    private void validateJson(RuleTestCaseDTO testCase) {
        readMap(testCase.getFactsJson(), "factsJson");
        validateOptionalJson(testCase.getExpectedHitRulesJson(), "expectedHitRulesJson");
        validateOptionalJson(testCase.getExpectedOutputsJson(), "expectedOutputsJson");
    }

    private void validateOptionalJson(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " is invalid JSON", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize test run result", e);
        }
    }

    private RuleTestCaseDTO toDTO(RuleTestCase testCase) {
        RuleTestCaseDTO dto = new RuleTestCaseDTO();
        dto.setId(testCase.getId());
        dto.setCaseCode(testCase.getCaseCode());
        dto.setCaseName(testCase.getCaseName());
        dto.setRuleCode(testCase.getRuleCode());
        dto.setVersion(testCase.getVersion());
        dto.setScenario(testCase.getScenario());
        dto.setFactsJson(testCase.getFactsJson());
        dto.setExpectedDecision(testCase.getExpectedDecision());
        dto.setExpectedHitRulesJson(testCase.getExpectedHitRulesJson());
        dto.setExpectedOutputsJson(testCase.getExpectedOutputsJson());
        dto.setEnabled(testCase.isEnabled());
        dto.setCreatedBy(testCase.getCreatedBy());
        dto.setCreatedAt(testCase.getCreatedAt());
        dto.setUpdatedAt(testCase.getUpdatedAt());
        return dto;
    }

    private RuleTestCase toDomain(RuleTestCaseDTO dto) {
        RuleTestCase testCase = new RuleTestCase();
        testCase.setId(dto.getId());
        testCase.setCaseCode(dto.getCaseCode());
        testCase.setCaseName(dto.getCaseName());
        testCase.setRuleCode(dto.getRuleCode());
        testCase.setVersion(dto.getVersion());
        testCase.setScenario(dto.getScenario());
        testCase.setFactsJson(dto.getFactsJson());
        testCase.setExpectedDecision(dto.getExpectedDecision());
        testCase.setExpectedHitRulesJson(dto.getExpectedHitRulesJson());
        testCase.setExpectedOutputsJson(dto.getExpectedOutputsJson());
        testCase.setEnabled(dto.isEnabled());
        testCase.setCreatedBy(dto.getCreatedBy());
        testCase.setCreatedAt(dto.getCreatedAt());
        testCase.setUpdatedAt(dto.getUpdatedAt());
        return testCase;
    }

    private RuleTestSuiteDTO toDTO(RuleTestSuite suite) {
        RuleTestSuiteDTO dto = new RuleTestSuiteDTO();
        dto.setId(suite.getId());
        dto.setSuiteCode(suite.getSuiteCode());
        dto.setSuiteName(suite.getSuiteName());
        dto.setRuleCode(suite.getRuleCode());
        dto.setBusinessLine(suite.getBusinessLine());
        dto.setDescription(suite.getDescription());
        dto.setEnabled(suite.isEnabled());
        dto.setCreatedBy(suite.getCreatedBy());
        dto.setCreatedAt(suite.getCreatedAt());
        dto.setUpdatedAt(suite.getUpdatedAt());
        dto.setCaseCodes(suite.getCaseCodes());
        return dto;
    }

    private RuleTestSuite toDomain(RuleTestSuiteDTO dto) {
        RuleTestSuite suite = new RuleTestSuite();
        suite.setId(dto.getId());
        suite.setSuiteCode(dto.getSuiteCode());
        suite.setSuiteName(dto.getSuiteName());
        suite.setRuleCode(dto.getRuleCode());
        suite.setBusinessLine(dto.getBusinessLine());
        suite.setDescription(dto.getDescription());
        suite.setEnabled(dto.isEnabled());
        suite.setCreatedBy(dto.getCreatedBy());
        suite.setCreatedAt(dto.getCreatedAt());
        suite.setUpdatedAt(dto.getUpdatedAt());
        suite.setCaseCodes(dto.getCaseCodes() == null ? new ArrayList<>() : dto.getCaseCodes());
        return suite;
    }

    private RuleTestRunDTO toDTO(RuleTestRun run) {
        RuleTestRunDTO dto = new RuleTestRunDTO();
        dto.setId(run.getId());
        dto.setRunId(run.getRunId());
        dto.setSuiteCode(run.getSuiteCode());
        dto.setCaseCode(run.getCaseCode());
        dto.setRuleCode(run.getRuleCode());
        dto.setStatus(run.getStatus());
        dto.setTotalCases(run.getTotalCases());
        dto.setPassedCases(run.getPassedCases());
        dto.setFailedCases(run.getFailedCases());
        dto.setResultJson(run.getResultJson());
        dto.setExecutedBy(run.getExecutedBy());
        dto.setStartedAt(run.getStartedAt());
        dto.setFinishedAt(run.getFinishedAt());
        return dto;
    }

    private record CaseResult(String caseCode, boolean passed, List<String> errors, RuleExecutionResultDTO actual) {
    }
}
