package com.insurance.ruleengine.client.api;

import com.insurance.ruleengine.client.dto.RuleTemplateDTO;

import java.util.List;
import java.util.Optional;

public interface TemplateFacade {
    List<RuleTemplateDTO> findAll();
    List<RuleTemplateDTO> findByCategory(String category);
    Optional<RuleTemplateDTO> findByCode(String templateCode);
    RuleTemplateDTO create(RuleTemplateDTO template);
    RuleTemplateDTO update(String templateCode, RuleTemplateDTO template);
    void delete(String templateCode);
}
