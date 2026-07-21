package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleAuditLogJpaRepository extends JpaRepository<RuleAuditLogEntity, Long> {
    List<RuleAuditLogEntity> findByRuleCodeOrderByCreatedAtDesc(String ruleCode);

    /** P1-3: returns audit entries for a rule ordered by id desc (used for hash-chain lookup). */
    List<RuleAuditLogEntity> findByRuleCodeOrderByIdDesc(String ruleCode);
}

