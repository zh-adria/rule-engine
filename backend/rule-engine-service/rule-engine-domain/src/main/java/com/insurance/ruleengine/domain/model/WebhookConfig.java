package com.insurance.ruleengine.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Webhook 配置
 */
public class WebhookConfig {

    private Long id;
    private String webhookUrl;
    private List<String> eventTypes;
    private String secret;
    private String description;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private WebhookConfig() {}

    public static WebhookConfig create(String webhookUrl, List<String> eventTypes,
                                        String secret, String description) {
        WebhookConfig config = new WebhookConfig();
        config.webhookUrl = webhookUrl;
        config.eventTypes = eventTypes;
        config.secret = secret;
        config.description = description;
        config.enabled = true;
        config.createdAt = LocalDateTime.now();
        config.updatedAt = LocalDateTime.now();
        return config;
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean shouldNotify(String eventType) {
        return enabled && eventTypes.contains(eventType);
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public List<String> getEventTypes() { return eventTypes; }
    public void setEventTypes(List<String> eventTypes) { this.eventTypes = eventTypes; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
