package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.WebhookConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookConfigJpaRepository extends JpaRepository<WebhookConfigEntity, Long> {

    List<WebhookConfigEntity> findByEnabledTrue();
}
