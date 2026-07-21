package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.WebhookLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookLogJpaRepository extends JpaRepository<WebhookLogEntity, Long> {

    List<WebhookLogEntity> findByWebhookIdOrderByCreatedAtDesc(Long webhookId);

    List<WebhookLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
