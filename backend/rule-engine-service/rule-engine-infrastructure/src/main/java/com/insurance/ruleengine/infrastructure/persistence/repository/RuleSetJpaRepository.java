package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RuleSetJpaRepository extends JpaRepository<RuleSetEntity, Long> {
    Optional<RuleSetEntity> findBySetCode(String setCode);

    boolean existsBySetCode(String setCode);
}
