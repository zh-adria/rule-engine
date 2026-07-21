package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTestSuiteCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleTestSuiteCaseJpaRepository extends JpaRepository<RuleTestSuiteCaseEntity, Long> {
    List<RuleTestSuiteCaseEntity> findBySuiteCodeOrderByCaseOrderAsc(String suiteCode);
    Optional<RuleTestSuiteCaseEntity> findBySuiteCodeAndCaseCode(String suiteCode, String caseCode);
}
