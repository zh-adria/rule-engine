package com.insurance.ruleengine.domain.gateway;

import com.insurance.ruleengine.domain.model.WebhookConfig;
import com.insurance.ruleengine.domain.model.WebhookLog;

import java.util.List;
import java.util.Optional;

/**
 * Webhook 网关接口
 */
public interface WebhookGateway {

    // Webhook Config operations
    WebhookConfig saveConfig(WebhookConfig config);

    Optional<WebhookConfig> findConfigById(Long id);

    List<WebhookConfig> findAllConfigs();

    List<WebhookConfig> findEnabledConfigs();

    void deleteConfig(Long id);

    // Webhook Log operations
    WebhookLog saveLog(WebhookLog log);

    List<WebhookLog> findLogsByWebhookId(Long webhookId);

    List<WebhookLog> findRecentLogs(int limit);

    /**
     * Phase 2: asynchronously notify all enabled webhooks subscribed to this event.
     * Logs each attempt (success/failure) to WebhookLog in the same transaction;
     * actual HTTP send is dispatched on a background thread pool.
     */
    void sendAsync(String eventType, String payload);
}
