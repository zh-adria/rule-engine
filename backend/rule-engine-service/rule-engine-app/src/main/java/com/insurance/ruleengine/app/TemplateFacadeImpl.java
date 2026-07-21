package com.insurance.ruleengine.app;

import com.insurance.ruleengine.client.api.TemplateFacade;
import com.insurance.ruleengine.client.dto.RuleTemplateDTO;
import com.insurance.ruleengine.domain.gateway.TemplateGateway;
import com.insurance.ruleengine.domain.model.RuleTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TemplateFacadeImpl implements TemplateFacade {
    private final TemplateGateway templateGateway;

    public TemplateFacadeImpl(TemplateGateway templateGateway) {
        this.templateGateway = templateGateway;
    }

    @Override
    public List<RuleTemplateDTO> findAll() {
        return templateGateway.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<RuleTemplateDTO> findByCategory(String category) {
        return templateGateway.findByCategory(category).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Optional<RuleTemplateDTO> findByCode(String templateCode) {
        return templateGateway.findByCode(templateCode).map(this::toDTO);
    }

    @Override
    public RuleTemplateDTO create(RuleTemplateDTO template) {
        RuleTemplate domain = toDomain(template);
        domain = templateGateway.create(domain);
        return toDTO(domain);
    }

    @Override
    public RuleTemplateDTO update(String templateCode, RuleTemplateDTO template) {
        return templateGateway.findByCode(templateCode)
                .map(existing -> {
                    existing.setTemplateName(template.getTemplateName());
                    existing.setCategory(template.getCategory());
                    existing.setBusinessLine(template.getBusinessLine());
                    existing.setDescription(template.getDescription());
                    existing.setDrlTemplate(template.getDrlTemplate());
                    existing.setVisualTemplate(template.getVisualTemplate());
                    existing.setSensitive(template.isSensitive());
                    existing.setOwner(template.getOwner());
                    existing.setSortOrder(template.getSortOrder());
                    return toDTO(templateGateway.update(existing));
                })
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateCode));
    }

    @Override
    public void delete(String templateCode) {
        templateGateway.delete(templateCode);
    }

    private RuleTemplateDTO toDTO(RuleTemplate template) {
        RuleTemplateDTO dto = new RuleTemplateDTO();
        dto.setId(template.getId());
        dto.setTemplateCode(template.getTemplateCode());
        dto.setTemplateName(template.getTemplateName());
        dto.setCategory(template.getCategory());
        dto.setBusinessLine(template.getBusinessLine());
        dto.setDescription(template.getDescription());
        dto.setDrlTemplate(template.getDrlTemplate());
        dto.setVisualTemplate(template.getVisualTemplate());
        dto.setSensitive(template.isSensitive());
        dto.setOwner(template.getOwner());
        dto.setSortOrder(template.getSortOrder());
        return dto;
    }

    private RuleTemplate toDomain(RuleTemplateDTO dto) {
        RuleTemplate t = new RuleTemplate();
        t.setId(dto.getId());
        t.setTemplateCode(dto.getTemplateCode());
        t.setTemplateName(dto.getTemplateName());
        t.setCategory(dto.getCategory());
        t.setBusinessLine(dto.getBusinessLine());
        t.setDescription(dto.getDescription());
        t.setDrlTemplate(dto.getDrlTemplate());
        t.setVisualTemplate(dto.getVisualTemplate());
        t.setSensitive(dto.isSensitive());
        t.setOwner(dto.getOwner());
        t.setSortOrder(dto.getSortOrder());
        return t;
    }
}
