package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RuleTemplateJpaRepository extends JpaRepository<RuleTemplateEntity, Long> {
    Optional<RuleTemplateEntity> findByTemplateCode(String templateCode);
    List<RuleTemplateEntity> findByCategory(String category);
    List<RuleTemplateEntity> findByBusinessLine(String businessLine);
    List<RuleTemplateEntity> findByCategoryAndBusinessLine(String category, String businessLine);
    boolean existsByTemplateCode(String templateCode);
}
