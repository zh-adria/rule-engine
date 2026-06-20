package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleVersionJpaRepository extends JpaRepository<RuleVersionEntity, Long> {
    Optional<RuleVersionEntity> findByRuleCodeAndVersion(String ruleCode, Integer version);

    Optional<RuleVersionEntity> findTopByRuleCodeOrderByVersionDesc(String ruleCode);

    List<RuleVersionEntity> findByRuleCodeOrderByVersionDesc(String ruleCode);
}

