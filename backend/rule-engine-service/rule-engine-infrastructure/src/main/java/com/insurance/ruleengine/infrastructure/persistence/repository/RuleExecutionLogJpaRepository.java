package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleExecutionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleExecutionLogJpaRepository extends JpaRepository<RuleExecutionLogEntity, Long> {
}

