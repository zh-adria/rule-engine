package com.insurance.ruleengine.infrastructure.persistence;

import com.insurance.ruleengine.domain.gateway.RuleGateway;
import com.insurance.ruleengine.domain.model.RuleCategory;
import com.insurance.ruleengine.domain.model.RuleDefinition;
import com.insurance.ruleengine.domain.model.RuleStatus;
import com.insurance.ruleengine.domain.model.RuleVersion;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleDefinitionEntity;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleVersionEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleDefinitionJpaRepository;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleVersionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RuleGatewayImpl implements RuleGateway {
    private final RuleDefinitionJpaRepository ruleRepository;
    private final RuleVersionJpaRepository versionRepository;

    public RuleGatewayImpl(RuleDefinitionJpaRepository ruleRepository, RuleVersionJpaRepository versionRepository) {
        this.ruleRepository = ruleRepository;
        this.versionRepository = versionRepository;
    }

    @Override
    public boolean existsRule(String ruleCode) {
        return ruleRepository.existsByRuleCode(ruleCode);
    }

    @Override
    public Optional<RuleDefinition> findRule(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode).map(this::toDomain);
    }

    @Override
    public Optional<RuleDefinition> lockRuleForUpdate(String ruleCode) {
        return ruleRepository.findByRuleCodeForUpdate(ruleCode).map(this::toDomain);
    }

    @Override
    public List<RuleDefinition> listRules(String category, String businessLine, String status, String keyword) {
        return ruleRepository.searchRules(blankToNull(category), blankToNull(businessLine),
                        archivedFilter(status), blankToNull(keyword))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public RuleDefinition saveRule(RuleDefinition rule) {
        return toDomain(ruleRepository.save(toEntity(rule)));
    }

    @Override
    public int nextVersion(String ruleCode) {
        return versionRepository.findTopByRuleCodeOrderByVersionDesc(ruleCode)
                .map(item -> item.getVersion() + 1)
                .orElse(1);
    }

    @Override
    public Optional<RuleVersion> findVersion(String ruleCode, Integer version) {
        return versionRepository.findByRuleCodeAndVersion(ruleCode, version).map(this::toDomain);
    }

    @Override
    public Optional<RuleVersion> findCurrentVersion(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode)
                .map(RuleDefinitionEntity::getCurrentVersion)
                .flatMap(version -> versionRepository.findByRuleCodeAndVersion(ruleCode, version))
                .map(this::toDomain);
    }

    @Override
    public List<RuleVersion> listVersions(String ruleCode) {
        return versionRepository.findByRuleCodeOrderByVersionDesc(ruleCode).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public RuleVersion saveVersion(RuleVersion version) {
        return toDomain(versionRepository.save(toEntity(version)));
    }

    private RuleDefinition toDomain(RuleDefinitionEntity entity) {
        RuleDefinition rule = new RuleDefinition();
        rule.setId(entity.getId());
        rule.setRuleCode(entity.getRuleCode());
        rule.setRuleName(entity.getRuleName());
        rule.setCategory(RuleCategory.valueOf(entity.getCategory()));
        rule.setBusinessLine(entity.getBusinessLine());
        rule.setDescription(entity.getDescription());
        rule.setSensitive(entity.isSensitive());
        rule.setArchived(entity.isArchived());
        rule.setOwner(entity.getOwner());
        rule.setCurrentVersion(entity.getCurrentVersion());
        rule.setGrayVersion(entity.getGrayVersion());
        rule.setGrayPercent(entity.getGrayPercent());
        rule.setRegulatoryRef(entity.getRegulatoryRef());
        return rule;
    }

    private RuleDefinitionEntity toEntity(RuleDefinition rule) {
        RuleDefinitionEntity entity = rule.getId() == null
                ? new RuleDefinitionEntity()
                : ruleRepository.findById(rule.getId()).orElseGet(RuleDefinitionEntity::new);
        entity.setId(rule.getId());
        entity.setRuleCode(rule.getRuleCode());
        entity.setRuleName(rule.getRuleName());
        entity.setCategory(rule.getCategory().name());
        entity.setBusinessLine(rule.getBusinessLine());
        entity.setDescription(rule.getDescription());
        entity.setSensitive(rule.isSensitive());
        entity.setArchived(rule.isArchived());
        entity.setOwner(rule.getOwner());
        entity.setCurrentVersion(rule.getCurrentVersion());
        entity.setGrayVersion(rule.getGrayVersion());
        entity.setGrayPercent(rule.getGrayPercent());
        entity.setRegulatoryRef(rule.getRegulatoryRef());
        return entity;
    }

    private RuleVersion toDomain(RuleVersionEntity entity) {
        RuleVersion version = new RuleVersion();
        version.setId(entity.getId());
        version.setRuleCode(entity.getRuleCode());
        version.setVersion(entity.getVersion());
        version.setStatus(RuleStatus.valueOf(entity.getStatus()));
        version.setDrlContent(entity.getDrlContent());
        version.setVisualModel(entity.getVisualModel());
        version.setChecksum(entity.getChecksum());
        version.setCreatedBy(entity.getCreatedBy());
        version.setApprovedBy(entity.getApprovedBy());
        version.setPublishedAt(entity.getPublishedAt());
        return version;
    }

    private RuleVersionEntity toEntity(RuleVersion version) {
        RuleVersionEntity entity = version.getId() == null
                ? versionRepository.findByRuleCodeAndVersion(version.getRuleCode(), version.getVersion()).orElseGet(RuleVersionEntity::new)
                : versionRepository.findById(version.getId()).orElseGet(RuleVersionEntity::new);
        entity.setId(version.getId());
        entity.setRuleCode(version.getRuleCode());
        entity.setVersion(version.getVersion());
        entity.setStatus(version.getStatus().name());
        entity.setDrlContent(version.getDrlContent());
        entity.setVisualModel(version.getVisualModel());
        entity.setChecksum(version.getChecksum());
        entity.setCreatedBy(version.getCreatedBy());
        entity.setApprovedBy(version.getApprovedBy());
        entity.setPublishedAt(version.getPublishedAt());
        return entity;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Boolean archivedFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if ("ARCHIVED".equalsIgnoreCase(status)) {
            return true;
        }
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return false;
        }
        return null;
    }
}
