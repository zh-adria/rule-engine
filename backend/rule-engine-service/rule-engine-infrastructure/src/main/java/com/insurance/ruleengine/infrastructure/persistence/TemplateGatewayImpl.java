package com.insurance.ruleengine.infrastructure.persistence;

import com.insurance.ruleengine.domain.gateway.TemplateGateway;
import com.insurance.ruleengine.domain.model.RuleTemplate;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTemplateEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleTemplateJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TemplateGatewayImpl implements TemplateGateway {
    private final RuleTemplateJpaRepository repository;

    public TemplateGatewayImpl(RuleTemplateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RuleTemplate> findAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> a.getSortOrder() - b.getSortOrder())
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RuleTemplate> findByCode(String templateCode) {
        return repository.findByTemplateCode(templateCode).map(this::toDomain);
    }

    @Override
    public List<RuleTemplate> findByCategory(String category) {
        return repository.findByCategory(category).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public RuleTemplate create(RuleTemplate template) {
        return toDomain(repository.save(toEntity(template)));
    }

    @Override
    public RuleTemplate update(RuleTemplate template) {
        return toDomain(repository.save(toEntity(template)));
    }

    @Override
    public void delete(String templateCode) {
        repository.findByTemplateCode(templateCode).ifPresent(repository::delete);
    }

    private RuleTemplate toDomain(RuleTemplateEntity entity) {
        RuleTemplate t = new RuleTemplate();
        t.setId(entity.getId());
        t.setTemplateCode(entity.getTemplateCode());
        t.setTemplateName(entity.getTemplateName());
        t.setCategory(entity.getCategory());
        t.setBusinessLine(entity.getBusinessLine());
        t.setDescription(entity.getDescription());
        t.setDrlTemplate(entity.getDrlTemplate());
        t.setVisualTemplate(entity.getVisualTemplate());
        t.setSensitive(entity.isSensitive());
        t.setOwner(entity.getOwner());
        t.setSortOrder(entity.getSortOrder());
        return t;
    }

    private RuleTemplateEntity toEntity(RuleTemplate template) {
        RuleTemplateEntity entity = new RuleTemplateEntity();
        entity.setId(template.getId());
        entity.setTemplateCode(template.getTemplateCode());
        entity.setTemplateName(template.getTemplateName());
        entity.setCategory(template.getCategory());
        entity.setBusinessLine(template.getBusinessLine());
        entity.setDescription(template.getDescription());
        entity.setDrlTemplate(template.getDrlTemplate());
        entity.setVisualTemplate(template.getVisualTemplate());
        entity.setSensitive(template.isSensitive());
        entity.setOwner(template.getOwner());
        entity.setSortOrder(template.getSortOrder());
        return entity;
    }
}
