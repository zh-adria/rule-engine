package com.insurance.ruleengine.infrastructure.persistence;

import com.insurance.ruleengine.domain.gateway.CustomFieldGateway;
import com.insurance.ruleengine.domain.model.CustomField;
import com.insurance.ruleengine.infrastructure.persistence.entity.CustomFieldEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.CustomFieldRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomFieldGatewayImpl implements CustomFieldGateway {

    private final CustomFieldRepository repository;

    public CustomFieldGatewayImpl(CustomFieldRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CustomField> findByBusinessLine(String businessLine) {
        return repository.findByBusinessLineAndEnabledTrueOrderBySortOrderAsc(businessLine).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomField> findAll() {
        return repository.findByEnabledTrueOrderBySortOrderAsc().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public CustomField save(CustomField field) {
        CustomFieldEntity entity;
        if (field.getId() != null) {
            // Update: preserve createdAt from existing entity
            entity = repository.findById(field.getId())
                    .orElseThrow(() -> new IllegalArgumentException("custom field not found: " + field.getId()));
            entity.setFieldCode(field.getFieldCode());
            entity.setFieldLabel(field.getFieldLabel());
            entity.setFieldType(field.getFieldType());
            entity.setBusinessLine(field.getBusinessLine());
            entity.setSortOrder(field.getSortOrder());
            entity.setEnabled(field.getEnabled());
        } else {
            // Create: new entity
            entity = toEntity(field);
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return toDomain(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private CustomField toDomain(CustomFieldEntity e) {
        CustomField f = new CustomField();
        f.setId(e.getId());
        f.setFieldCode(e.getFieldCode());
        f.setFieldLabel(e.getFieldLabel());
        f.setFieldType(e.getFieldType());
        f.setBusinessLine(e.getBusinessLine());
        f.setSortOrder(e.getSortOrder());
        f.setEnabled(e.getEnabled());
        return f;
    }

    private CustomFieldEntity toEntity(CustomField f) {
        CustomFieldEntity e = new CustomFieldEntity();
        e.setId(f.getId());
        e.setFieldCode(f.getFieldCode());
        e.setFieldLabel(f.getFieldLabel());
        e.setFieldType(f.getFieldType());
        e.setBusinessLine(f.getBusinessLine());
        e.setSortOrder(f.getSortOrder());
        e.setEnabled(f.getEnabled());
        return e;
    }
}
