package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTestCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleTestCaseJpaRepository extends JpaRepository<RuleTestCaseEntity, Long> {
    Optional<RuleTestCaseEntity> findByCaseCode(String caseCode);
    List<RuleTestCaseEntity> findByRuleCode(String ruleCode);
    List<RuleTestCaseEntity> findByEnabled(boolean enabled);
    List<RuleTestCaseEntity> findByRuleCodeAndEnabled(String ruleCode, boolean enabled);
}
