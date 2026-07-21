package com.insurance.ruleengine.infrastructure.persistence;

import com.insurance.ruleengine.domain.gateway.RuleSetGateway;
import com.insurance.ruleengine.domain.model.ExecutionMode;
import com.insurance.ruleengine.domain.model.RuleSet;
import com.insurance.ruleengine.domain.model.RuleSetStep;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleSetEntity;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleSetStepEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleSetJpaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RuleSetGatewayImpl implements RuleSetGateway {
    private final RuleSetJpaRepository ruleSetRepository;

    public RuleSetGatewayImpl(RuleSetJpaRepository ruleSetRepository) {
        this.ruleSetRepository = ruleSetRepository;
    }

    @Override
    public boolean existsRuleSet(String setCode) {
        return ruleSetRepository.existsBySetCode(setCode);
    }

    @Override
    public Optional<RuleSet> findRuleSet(String setCode) {
        return ruleSetRepository.findBySetCode(setCode).map(this::toDomain);
    }

    @Override
    public RuleSet saveRuleSet(RuleSet ruleSet) {
        return toDomain(ruleSetRepository.save(toEntity(ruleSet)));
    }

    @Override
    public List<RuleSet> findAllRuleSets() {
        return ruleSetRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteRuleSet(String setCode) {
        ruleSetRepository.findBySetCode(setCode).ifPresent(ruleSetRepository::delete);
    }

    @Override
    public RuleSet updateRuleSet(String setCode, RuleSet updated) {
        RuleSetEntity existing = ruleSetRepository.findBySetCode(setCode)
                .orElseThrow(() -> new IllegalArgumentException("rule set not found: " + setCode));
        existing.setSetName(updated.getSetName());
        existing.setDescription(updated.getDescription());
        existing.setOwner(updated.getOwner());
        existing.getSteps().clear();
        if (updated.getSteps() != null) {
            for (RuleSetStep step : updated.getSteps()) {
                RuleSetStepEntity stepEntity = new RuleSetStepEntity();
                stepEntity.setId(step.getId());
                stepEntity.setRuleSet(existing);
                stepEntity.setStepOrder(step.getStepOrder());
                stepEntity.setRuleCode(step.getRuleCode());
                stepEntity.setRuleVersion(step.getRuleVersion());
                stepEntity.setMode(step.getMode().name());
                stepEntity.setStopOnDecline(step.isStopOnDecline());
                existing.getSteps().add(stepEntity);
            }
        }
        return toDomain(ruleSetRepository.save(existing));
    }

    private RuleSet toDomain(RuleSetEntity entity) {
        RuleSet ruleSet = new RuleSet();
        ruleSet.setId(entity.getId());
        ruleSet.setSetCode(entity.getSetCode());
        ruleSet.setSetName(entity.getSetName());
        ruleSet.setDescription(entity.getDescription());
        ruleSet.setOwner(entity.getOwner());
        if (entity.getSteps() != null) {
            List<RuleSetStep> steps = entity.getSteps().stream()
                    .map(this::toDomainStep)
                    .collect(Collectors.toList());
            ruleSet.setSteps(steps);
        }
        return ruleSet;
    }

    private RuleSetStep toDomainStep(RuleSetStepEntity entity) {
        RuleSetStep step = new RuleSetStep();
        step.setId(entity.getId());
        step.setSetCode(entity.getRuleSet() != null ? entity.getRuleSet().getSetCode() : null);
        step.setStepOrder(entity.getStepOrder());
        step.setRuleCode(entity.getRuleCode());
        step.setRuleVersion(entity.getRuleVersion());
        step.setMode(ExecutionMode.valueOf(entity.getMode()));
        step.setStopOnDecline(entity.isStopOnDecline());
        return step;
    }

    private RuleSetEntity toEntity(RuleSet ruleSet) {
        RuleSetEntity entity;
        if (ruleSet.getId() != null) {
            entity = ruleSetRepository.findById(ruleSet.getId()).orElseGet(RuleSetEntity::new);
        } else {
            entity = ruleSetRepository.findBySetCode(ruleSet.getSetCode()).orElseGet(RuleSetEntity::new);
        }
        entity.setId(ruleSet.getId());
        entity.setSetCode(ruleSet.getSetCode());
        entity.setSetName(ruleSet.getSetName());
        entity.setDescription(ruleSet.getDescription());
        entity.setOwner(ruleSet.getOwner());

        // Sync steps
        entity.getSteps().clear();
        if (ruleSet.getSteps() != null) {
            for (RuleSetStep step : ruleSet.getSteps()) {
                RuleSetStepEntity stepEntity = new RuleSetStepEntity();
                stepEntity.setId(step.getId());
                stepEntity.setRuleSet(entity);
                stepEntity.setStepOrder(step.getStepOrder());
                stepEntity.setRuleCode(step.getRuleCode());
                stepEntity.setRuleVersion(step.getRuleVersion());
                stepEntity.setMode(step.getMode().name());
                stepEntity.setStopOnDecline(step.isStopOnDecline());
                entity.getSteps().add(stepEntity);
            }
        }
        return entity;
    }
}
