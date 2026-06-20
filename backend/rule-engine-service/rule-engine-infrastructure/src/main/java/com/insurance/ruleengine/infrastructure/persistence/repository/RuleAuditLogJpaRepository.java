package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleAuditLogJpaRepository extends JpaRepository<RuleAuditLogEntity, Long> {
}

