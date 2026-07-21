package com.insurance.ruleengine.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.domain.gateway.WebhookGateway;
import com.insurance.ruleengine.domain.model.WebhookConfig;
import com.insurance.ruleengine.domain.model.WebhookLog;
import com.insurance.ruleengine.infrastructure.persistence.entity.WebhookConfigEntity;
import com.insurance.ruleengine.infrastructure.persistence.entity.WebhookLogEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.WebhookConfigJpaRepository;
import com.insurance.ruleengine.infrastructure.persistence.repository.WebhookLogJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
public class WebhookGatewayImpl implements WebhookGateway {
    private static final Logger log = LoggerFactory.getLogger(WebhookGatewayImpl.class);

    private final WebhookConfigJpaRepository configRepository;
    private final WebhookLogJpaRepository logRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private ExecutorService executor;

    public WebhookGatewayImpl(WebhookConfigJpaRepository configRepository,
                               WebhookLogJpaRepository logRepository,
                               ObjectMapper objectMapper) {
        this.configRepository = configRepository;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.executor = Executors.newFixedThreadPool(4);
    }

    /** Package-visible constructor for tests. */
    WebhookGatewayImpl(WebhookConfigJpaRepository configRepository,
                       WebhookLogJpaRepository logRepository,
                       ObjectMapper objectMapper,
                       RestTemplate restTemplate,
                       ExecutorService executor) {
        this.configRepository = configRepository;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.executor = executor;
    }

    @Override
    public WebhookConfig saveConfig(WebhookConfig config) {
        WebhookConfigEntity entity = toConfigEntity(config);
        entity = configRepository.save(entity);
        return toConfigDomain(entity);
    }

    @Override
    public Optional<WebhookConfig> findConfigById(Long id) {
        return configRepository.findById(id).map(this::toConfigDomain);
    }

    @Override
    public List<WebhookConfig> findAllConfigs() {
        return configRepository.findAll().stream()
                .map(this::toConfigDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<WebhookConfig> findEnabledConfigs() {
        return configRepository.findByEnabledTrue().stream()
                .map(this::toConfigDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteConfig(Long id) {
        configRepository.deleteById(id);
    }

    @Override
    public WebhookLog saveLog(WebhookLog log) {
        WebhookLogEntity entity = toLogEntity(log);
        entity = logRepository.save(entity);
        return toLogDomain(entity);
    }

    @Override
    public List<WebhookLog> findLogsByWebhookId(Long webhookId) {
        return logRepository.findByWebhookIdOrderByCreatedAtDesc(webhookId).stream()
                .map(this::toLogDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<WebhookLog> findRecentLogs(int limit) {
        return logRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(this::toLogDomain)
                .collect(Collectors.toList());
    }

    private WebhookConfigEntity toConfigEntity(WebhookConfig config) {
        WebhookConfigEntity entity = new WebhookConfigEntity();
        entity.setId(config.getId());
        entity.setWebhookUrl(config.getWebhookUrl());
        try {
            entity.setEventTypes(objectMapper.writeValueAsString(config.getEventTypes()));
        } catch (JsonProcessingException e) {
            entity.setEventTypes("[]");
        }
        entity.setSecret(config.getSecret());
        entity.setDescription(config.getDescription());
        entity.setEnabled(config.isEnabled());
        entity.setCreatedAt(config.getCreatedAt());
        entity.setUpdatedAt(config.getUpdatedAt());
        return entity;
    }

    private WebhookConfig toConfigDomain(WebhookConfigEntity entity) {
        WebhookConfig config = WebhookConfig.create(
                entity.getWebhookUrl(),
                parseEventTypes(entity.getEventTypes()),
                entity.getSecret(),
                entity.getDescription()
        );
        config.setId(entity.getId());
        config.setEnabled(entity.getEnabled());
        return config;
    }

    private List<String> parseEventTypes(String eventTypesJson) {
        try {
            return objectMapper.readValue(eventTypesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private WebhookLogEntity toLogEntity(WebhookLog log) {
        WebhookLogEntity entity = new WebhookLogEntity();
        entity.setId(log.getId());
        entity.setWebhookId(log.getWebhookId());
        entity.setEventType(log.getEventType());
        entity.setRuleCode(log.getRuleCode());
        entity.setRequestUrl(log.getRequestUrl());
        entity.setRequestBody(log.getRequestBody());
        entity.setResponseStatus(log.getResponseStatus());
        entity.setResponseBody(log.getResponseBody());
        entity.setSuccess(log.isSuccess());
        entity.setErrorMessage(log.getErrorMessage());
        entity.setCreatedAt(log.getCreatedAt());
        return entity;
    }

    private WebhookLog toLogDomain(WebhookLogEntity entity) {
        WebhookLog log = WebhookLog.create(
                entity.getWebhookId(),
                entity.getEventType(),
                entity.getRuleCode(),
                entity.getRequestUrl(),
                entity.getRequestBody()
        );
        log.setId(entity.getId());
        if (entity.getSuccess()) {
            log.markSuccess(entity.getResponseStatus(), entity.getResponseBody());
        } else {
            log.markFailed(entity.getErrorMessage());
        }
        return log;
    }

    // ---- Phase 2: async webhook dispatch ----

    @Override
    public void sendAsync(String eventType, String payload) {
        List<WebhookConfig> subscribers = findEnabledConfigs().stream()
                .filter(c -> c.shouldNotify(eventType))
                .collect(Collectors.toList());
        if (subscribers.isEmpty()) {
            return;
        }
        for (WebhookConfig cfg : subscribers) {
            // Persist a PENDING log synchronously so the caller's transaction records the attempt.
            WebhookLog pendingLog = WebhookLog.create(
                    cfg.getId(), eventType, extractRuleCode(payload), cfg.getWebhookUrl(), payload);
            WebhookLog saved = saveLog(pendingLog);
            // Dispatch HTTP POST asynchronously.
            CompletableFuture.runAsync(() -> sendOne(cfg, payload, saved), executor)
                    .whenComplete((v, err) -> {
                        if (err != null) {
                            log.warn("webhook id={} event={} failed: {}", cfg.getId(), eventType, err.getMessage());
                        }
                    });
        }
    }

    private void sendOne(WebhookConfig cfg, String payload, WebhookLog logEntry) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cfg.getSecret() != null && !cfg.getSecret().isBlank()) {
            headers.set("X-Webhook-Secret", cfg.getSecret());
        }
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            org.springframework.http.ResponseEntity<String> resp =
                    restTemplate.postForEntity(cfg.getWebhookUrl(), entity, String.class);
            WebhookLog updated = WebhookLog.create(
                    logEntry.getWebhookId(), logEntry.getEventType(), logEntry.getRuleCode(),
                    logEntry.getRequestUrl(), logEntry.getRequestBody());
            updated.setId(logEntry.getId());
            updated.markSuccess(resp.getStatusCodeValue(), resp.getBody() != null ? resp.getBody() : "");
            saveLog(updated);
        } catch (Exception e) {
            WebhookLog updated = WebhookLog.create(
                    logEntry.getWebhookId(), logEntry.getEventType(), logEntry.getRuleCode(),
                    logEntry.getRequestUrl(), logEntry.getRequestBody());
            updated.setId(logEntry.getId());
            updated.markFailed(e.getMessage());
            saveLog(updated);
        }
    }

    private String extractRuleCode(String payload) {
        if (payload == null) return null;
        // naive extraction: look for "ruleCode":"..." or "ruleCode": "..."
        try {
            int i = payload.indexOf("ruleCode");
            if (i < 0) return null;
            int colon = payload.indexOf(':', i);
            int quote1 = payload.indexOf('"', colon);
            int quote2 = payload.indexOf('"', quote1 + 1);
            if (quote1 >= 0 && quote2 > quote1) {
                return payload.substring(quote1 + 1, quote2);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
