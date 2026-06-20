package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleDefinitionEntity;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface RuleDefinitionJpaRepository extends JpaRepository<RuleDefinitionEntity, Long> {
    Optional<RuleDefinitionEntity> findByRuleCode(String ruleCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rule from RuleDefinitionEntity rule where rule.ruleCode = :ruleCode")
    Optional<RuleDefinitionEntity> findByRuleCodeForUpdate(@Param("ruleCode") String ruleCode);

    boolean existsByRuleCode(String ruleCode);
}
