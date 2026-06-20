package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleAuditLogJpaRepository extends JpaRepository<RuleAuditLogEntity, Long> {
    List<RuleAuditLogEntity> findTop100ByRuleCodeOrderByCreatedAtDesc(String ruleCode);
}

