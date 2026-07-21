package com.insurance.ruleengine.domain.gateway;

import com.insurance.ruleengine.domain.model.RuleTemplate;

import java.util.List;
import java.util.Optional;

public interface TemplateGateway {
    List<RuleTemplate> findAll();
    Optional<RuleTemplate> findByCode(String templateCode);
    List<RuleTemplate> findByCategory(String category);
    RuleTemplate create(RuleTemplate template);
    RuleTemplate update(RuleTemplate template);
    void delete(String templateCode);
}
