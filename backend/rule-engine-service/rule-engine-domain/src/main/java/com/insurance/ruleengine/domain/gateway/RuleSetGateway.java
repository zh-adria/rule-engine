package com.insurance.ruleengine.domain.gateway;

import com.insurance.ruleengine.domain.model.RuleSet;

import java.util.List;
import java.util.Optional;

public interface RuleSetGateway {
    boolean existsRuleSet(String setCode);

    Optional<RuleSet> findRuleSet(String setCode);

    RuleSet saveRuleSet(RuleSet ruleSet);

    List<RuleSet> findAllRuleSets();

    RuleSet updateRuleSet(String setCode, RuleSet updated);

    void deleteRuleSet(String setCode);
}
