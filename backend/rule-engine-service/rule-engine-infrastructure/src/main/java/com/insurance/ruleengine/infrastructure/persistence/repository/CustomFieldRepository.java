package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.CustomFieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CustomFieldRepository extends JpaRepository<CustomFieldEntity, Long>, JpaSpecificationExecutor<CustomFieldEntity> {
    List<CustomFieldEntity> findByBusinessLineAndEnabledTrueOrderBySortOrderAsc(String businessLine);
    List<CustomFieldEntity> findByEnabledTrueOrderBySortOrderAsc();
}
