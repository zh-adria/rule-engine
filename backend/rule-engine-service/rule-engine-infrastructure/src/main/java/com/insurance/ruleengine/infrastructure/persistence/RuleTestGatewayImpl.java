package com.insurance.ruleengine.infrastructure.persistence;

import com.insurance.ruleengine.domain.gateway.RuleTestGateway;
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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RuleTestGatewayImpl implements RuleTestGateway {
    private final RuleTestCaseJpaRepository caseRepository;
    private final RuleTestSuiteJpaRepository suiteRepository;
    private final RuleTestSuiteCaseJpaRepository suiteCaseRepository;
    private final RuleTestRunJpaRepository runRepository;

    public RuleTestGatewayImpl(RuleTestCaseJpaRepository caseRepository,
                               RuleTestSuiteJpaRepository suiteRepository,
                               RuleTestSuiteCaseJpaRepository suiteCaseRepository,
                               RuleTestRunJpaRepository runRepository) {
        this.caseRepository = caseRepository;
        this.suiteRepository = suiteRepository;
        this.suiteCaseRepository = suiteCaseRepository;
        this.runRepository = runRepository;
    }

    @Override
    public RuleTestCase saveCase(RuleTestCase testCase) {
        return toDomain(caseRepository.save(toEntity(testCase)));
    }

    @Override
    public Optional<RuleTestCase> findCase(String caseCode) {
        return caseRepository.findByCaseCode(caseCode).map(this::toDomain);
    }

    @Override
    public List<RuleTestCase> listCases(String ruleCode, Boolean enabled) {
        if (ruleCode != null && enabled != null) {
            return caseRepository.findByRuleCodeAndEnabled(ruleCode, enabled).stream().map(this::toDomain).collect(Collectors.toList());
        }
        if (ruleCode != null) {
            return caseRepository.findByRuleCode(ruleCode).stream().map(this::toDomain).collect(Collectors.toList());
        }
        if (enabled != null) {
            return caseRepository.findByEnabled(enabled).stream().map(this::toDomain).collect(Collectors.toList());
        }
        return caseRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteCase(String caseCode) {
        caseRepository.findByCaseCode(caseCode).ifPresent(caseRepository::delete);
    }

    @Override
    public RuleTestSuite saveSuite(RuleTestSuite suite) {
        RuleTestSuiteEntity saved = suiteRepository.save(toEntity(suite));
        if (suite.getCaseCodes() != null) {
            syncSuiteCases(saved.getSuiteCode(), suite.getCaseCodes());
        }
        return toDomain(saved);
    }

    @Override
    public Optional<RuleTestSuite> findSuite(String suiteCode) {
        return suiteRepository.findBySuiteCode(suiteCode).map(this::toDomain);
    }

    @Override
    public List<RuleTestSuite> listSuites(String ruleCode, String businessLine, Boolean enabled) {
        if (ruleCode != null && businessLine != null && enabled != null) {
            return suiteRepository.findByRuleCodeAndBusinessLineAndEnabled(ruleCode, businessLine, enabled)
                    .stream().map(this::toDomain).collect(Collectors.toList());
        }
        if (ruleCode != null) {
            return suiteRepository.findByRuleCode(ruleCode).stream().map(this::toDomain).collect(Collectors.toList());
        }
        if (businessLine != null) {
            return suiteRepository.findByBusinessLine(businessLine).stream().map(this::toDomain).collect(Collectors.toList());
        }
        if (enabled != null) {
            return suiteRepository.findByEnabled(enabled).stream().map(this::toDomain).collect(Collectors.toList());
        }
        return suiteRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteSuite(String suiteCode) {
        suiteRepository.findBySuiteCode(suiteCode).ifPresent(suiteRepository::delete);
    }

    @Override
    public void addCaseToSuite(String suiteCode, String caseCode, int caseOrder) {
        RuleTestSuiteCaseEntity entity = suiteCaseRepository.findBySuiteCodeAndCaseCode(suiteCode, caseCode)
                .orElseGet(RuleTestSuiteCaseEntity::new);
        entity.setSuiteCode(suiteCode);
        entity.setCaseCode(caseCode);
        entity.setCaseOrder(caseOrder);
        suiteCaseRepository.save(entity);
    }

    @Override
    public void removeCaseFromSuite(String suiteCode, String caseCode) {
        suiteCaseRepository.findBySuiteCodeAndCaseCode(suiteCode, caseCode).ifPresent(suiteCaseRepository::delete);
    }

    @Override
    public List<RuleTestCase> listSuiteCases(String suiteCode) {
        return suiteCaseRepository.findBySuiteCodeOrderByCaseOrderAsc(suiteCode).stream()
                .map(RuleTestSuiteCaseEntity::getCaseCode)
                .map(caseRepository::findByCaseCode)
                .flatMap(Optional::stream)
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public RuleTestRun saveRun(RuleTestRun run) {
        return toDomain(runRepository.save(toEntity(run)));
    }

    @Override
    public Optional<RuleTestRun> findRun(String runId) {
        return runRepository.findByRunId(runId).map(this::toDomain);
    }

    @Override
    public List<RuleTestRun> listRuns(String ruleCode, String suiteCode, String caseCode) {
        if (ruleCode != null) {
            return runRepository.findByRuleCode(ruleCode).stream().map(this::toDomain).collect(Collectors.toList());
        }
        if (suiteCode != null) {
            return runRepository.findBySuiteCode(suiteCode).stream().map(this::toDomain).collect(Collectors.toList());
        }
        if (caseCode != null) {
            return runRepository.findByCaseCode(caseCode).stream().map(this::toDomain).collect(Collectors.toList());
        }
        return runRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    private RuleTestCase toDomain(RuleTestCaseEntity entity) {
        RuleTestCase testCase = new RuleTestCase();
        testCase.setId(entity.getId());
        testCase.setCaseCode(entity.getCaseCode());
        testCase.setCaseName(entity.getCaseName());
        testCase.setRuleCode(entity.getRuleCode());
        testCase.setVersion(entity.getVersion());
        testCase.setScenario(entity.getScenario());
        testCase.setFactsJson(entity.getFactsJson());
        testCase.setExpectedDecision(entity.getExpectedDecision());
        testCase.setExpectedHitRulesJson(entity.getExpectedHitRulesJson());
        testCase.setExpectedOutputsJson(entity.getExpectedOutputsJson());
        testCase.setEnabled(entity.isEnabled());
        testCase.setCreatedBy(entity.getCreatedBy());
        testCase.setCreatedAt(entity.getCreatedAt());
        testCase.setUpdatedAt(entity.getUpdatedAt());
        return testCase;
    }

    private RuleTestCaseEntity toEntity(RuleTestCase testCase) {
        RuleTestCaseEntity entity = new RuleTestCaseEntity();
        entity.setId(testCase.getId());
        entity.setCaseCode(testCase.getCaseCode());
        entity.setCaseName(testCase.getCaseName());
        entity.setRuleCode(testCase.getRuleCode());
        entity.setVersion(testCase.getVersion());
        entity.setScenario(testCase.getScenario());
        entity.setFactsJson(testCase.getFactsJson());
        entity.setExpectedDecision(testCase.getExpectedDecision());
        entity.setExpectedHitRulesJson(testCase.getExpectedHitRulesJson());
        entity.setExpectedOutputsJson(testCase.getExpectedOutputsJson());
        entity.setEnabled(testCase.isEnabled());
        entity.setCreatedBy(testCase.getCreatedBy());
        entity.setCreatedAt(testCase.getCreatedAt());
        entity.setUpdatedAt(testCase.getUpdatedAt());
        return entity;
    }

    private RuleTestSuite toDomain(RuleTestSuiteEntity entity) {
        RuleTestSuite suite = new RuleTestSuite();
        suite.setId(entity.getId());
        suite.setSuiteCode(entity.getSuiteCode());
        suite.setSuiteName(entity.getSuiteName());
        suite.setRuleCode(entity.getRuleCode());
        suite.setBusinessLine(entity.getBusinessLine());
        suite.setDescription(entity.getDescription());
        suite.setEnabled(entity.isEnabled());
        suite.setCreatedBy(entity.getCreatedBy());
        suite.setCreatedAt(entity.getCreatedAt());
        suite.setUpdatedAt(entity.getUpdatedAt());
        suite.setCaseCodes(listSuiteCaseCodes(entity.getSuiteCode()));
        return suite;
    }

    private void syncSuiteCases(String suiteCode, List<String> caseCodes) {
        List<RuleTestSuiteCaseEntity> existing = suiteCaseRepository.findBySuiteCodeOrderByCaseOrderAsc(suiteCode);
        if (existing != null) {
            existing.forEach(suiteCaseRepository::delete);
        }
        for (int i = 0; i < caseCodes.size(); i++) {
            addCaseToSuite(suiteCode, caseCodes.get(i), i + 1);
        }
    }

    private List<String> listSuiteCaseCodes(String suiteCode) {
        List<RuleTestSuiteCaseEntity> links = suiteCaseRepository.findBySuiteCodeOrderByCaseOrderAsc(suiteCode);
        if (links == null) {
            return List.of();
        }
        return links.stream()
                .map(RuleTestSuiteCaseEntity::getCaseCode)
                .collect(Collectors.toList());
    }

    private RuleTestSuiteEntity toEntity(RuleTestSuite suite) {
        RuleTestSuiteEntity entity = new RuleTestSuiteEntity();
        entity.setId(suite.getId());
        entity.setSuiteCode(suite.getSuiteCode());
        entity.setSuiteName(suite.getSuiteName());
        entity.setRuleCode(suite.getRuleCode());
        entity.setBusinessLine(suite.getBusinessLine());
        entity.setDescription(suite.getDescription());
        entity.setEnabled(suite.isEnabled());
        entity.setCreatedBy(suite.getCreatedBy());
        entity.setCreatedAt(suite.getCreatedAt());
        entity.setUpdatedAt(suite.getUpdatedAt());
        return entity;
    }

    private RuleTestRun toDomain(RuleTestRunEntity entity) {
        RuleTestRun run = new RuleTestRun();
        run.setId(entity.getId());
        run.setRunId(entity.getRunId());
        run.setSuiteCode(entity.getSuiteCode());
        run.setCaseCode(entity.getCaseCode());
        run.setRuleCode(entity.getRuleCode());
        run.setStatus(entity.getStatus());
        run.setTotalCases(entity.getTotalCases());
        run.setPassedCases(entity.getPassedCases());
        run.setFailedCases(entity.getFailedCases());
        run.setResultJson(entity.getResultJson());
        run.setExecutedBy(entity.getExecutedBy());
        run.setStartedAt(entity.getStartedAt());
        run.setFinishedAt(entity.getFinishedAt());
        return run;
    }

    private RuleTestRunEntity toEntity(RuleTestRun run) {
        RuleTestRunEntity entity = new RuleTestRunEntity();
        entity.setId(run.getId());
        entity.setRunId(run.getRunId());
        entity.setSuiteCode(run.getSuiteCode());
        entity.setCaseCode(run.getCaseCode());
        entity.setRuleCode(run.getRuleCode());
        entity.setStatus(run.getStatus());
        entity.setTotalCases(run.getTotalCases());
        entity.setPassedCases(run.getPassedCases());
        entity.setFailedCases(run.getFailedCases());
        entity.setResultJson(run.getResultJson());
        entity.setExecutedBy(run.getExecutedBy());
        entity.setStartedAt(run.getStartedAt());
        entity.setFinishedAt(run.getFinishedAt());
        return entity;
    }
}
