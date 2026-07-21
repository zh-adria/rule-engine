package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTestRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleTestRunJpaRepository extends JpaRepository<RuleTestRunEntity, Long> {
    Optional<RuleTestRunEntity> findByRunId(String runId);
    List<RuleTestRunEntity> findByRuleCode(String ruleCode);
    List<RuleTestRunEntity> findBySuiteCode(String suiteCode);
    List<RuleTestRunEntity> findByCaseCode(String caseCode);
}
