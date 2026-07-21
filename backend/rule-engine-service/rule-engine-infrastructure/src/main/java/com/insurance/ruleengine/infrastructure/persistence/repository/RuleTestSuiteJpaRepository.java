package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTestSuiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleTestSuiteJpaRepository extends JpaRepository<RuleTestSuiteEntity, Long> {
    Optional<RuleTestSuiteEntity> findBySuiteCode(String suiteCode);
    List<RuleTestSuiteEntity> findByRuleCode(String ruleCode);
    List<RuleTestSuiteEntity> findByBusinessLine(String businessLine);
    List<RuleTestSuiteEntity> findByEnabled(boolean enabled);
    List<RuleTestSuiteEntity> findByRuleCodeAndBusinessLineAndEnabled(String ruleCode, String businessLine, boolean enabled);
}
