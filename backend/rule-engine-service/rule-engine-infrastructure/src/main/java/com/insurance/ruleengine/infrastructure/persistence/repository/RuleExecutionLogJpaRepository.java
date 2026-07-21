package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleExecutionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleExecutionLogJpaRepository extends JpaRepository<RuleExecutionLogEntity, Long> {
    List<RuleExecutionLogEntity> findByRuleCodeOrderByCreatedAtDesc(String ruleCode);
}

