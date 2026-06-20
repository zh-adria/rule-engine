package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.RuleDefinitionEntity;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RuleDefinitionJpaRepository extends JpaRepository<RuleDefinitionEntity, Long> {
    Optional<RuleDefinitionEntity> findByRuleCode(String ruleCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rule from RuleDefinitionEntity rule where rule.ruleCode = :ruleCode")
    Optional<RuleDefinitionEntity> findByRuleCodeForUpdate(@Param("ruleCode") String ruleCode);

    @Query("select rule from RuleDefinitionEntity rule where " +
            "(:category is null or rule.category = :category) and " +
            "(:businessLine is null or rule.businessLine = :businessLine) and " +
            "(:archived is null or rule.archived = :archived) and " +
            "(:keyword is null or lower(rule.ruleCode) like lower(concat('%', :keyword, '%')) " +
            "or lower(rule.ruleName) like lower(concat('%', :keyword, '%'))) " +
            "order by rule.updatedAt desc, rule.id desc")
    List<RuleDefinitionEntity> searchRules(@Param("category") String category,
                                           @Param("businessLine") String businessLine,
                                           @Param("archived") Boolean archived,
                                           @Param("keyword") String keyword);

    boolean existsByRuleCode(String ruleCode);
}
