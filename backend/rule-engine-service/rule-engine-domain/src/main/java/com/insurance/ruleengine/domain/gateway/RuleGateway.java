package com.insurance.ruleengine.domain.gateway;

import com.insurance.ruleengine.domain.model.RuleDefinition;
import com.insurance.ruleengine.domain.model.RuleVersion;

import java.util.List;
import java.util.Optional;

public interface RuleGateway {
    boolean existsRule(String ruleCode);

    Optional<RuleDefinition> findRule(String ruleCode);

    Optional<RuleDefinition> lockRuleForUpdate(String ruleCode);

    List<RuleDefinition> listRules(String category, String businessLine, String status, String keyword);

    RuleDefinition saveRule(RuleDefinition rule);

    int nextVersion(String ruleCode);

    Optional<RuleVersion> findVersion(String ruleCode, Integer version);

    Optional<RuleVersion> findCurrentVersion(String ruleCode);

    List<RuleVersion> listVersions(String ruleCode);

    RuleVersion saveVersion(RuleVersion version);
}
