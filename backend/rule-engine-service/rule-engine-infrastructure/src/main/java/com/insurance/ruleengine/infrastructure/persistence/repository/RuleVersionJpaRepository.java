package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RuleVersionJpaRepository extends JpaRepository<RuleVersionEntity, Long> {
    Optional<RuleVersionEntity> findByRuleCodeAndVersion(String ruleCode, Integer version);

    Optional<RuleVersionEntity> findTopByRuleCodeOrderByVersionDesc(String ruleCode);
}

