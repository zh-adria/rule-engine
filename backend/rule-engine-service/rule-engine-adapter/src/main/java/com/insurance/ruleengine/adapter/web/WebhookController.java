package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.dto.CreateWebhookCmd;
import com.insurance.ruleengine.client.dto.WebhookConfigDTO;
import com.insurance.ruleengine.domain.gateway.WebhookGateway;
import com.insurance.ruleengine.domain.model.WebhookConfig;
import com.insurance.ruleengine.domain.model.WebhookLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookGateway webhookGateway;

    public WebhookController(WebhookGateway webhookGateway) {
        this.webhookGateway = webhookGateway;
    }

    @Operation(summary = "创建 Webhook 订阅")
    @PostMapping
    public ResponseEntity<WebhookConfigDTO> createWebhook(@Valid @RequestBody CreateWebhookCmd cmd) {
        WebhookConfig config = WebhookConfig.create(
                cmd.getWebhookUrl(),
                cmd.getEventTypes(),
                cmd.getSecret(),
                cmd.getDescription()
        );
        WebhookConfig saved = webhookGateway.saveConfig(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @Operation(summary = "列出所有 Webhook 订阅")
    @GetMapping
    public List<WebhookConfigDTO> listWebhooks() {
        return webhookGateway.findAllConfigs().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "查询 Webhook 详情")
    @GetMapping("/{id}")
    public WebhookConfigDTO getWebhook(@PathVariable Long id) {
        WebhookConfig config = webhookGateway.findConfigById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook不存在: " + id));
        return toDTO(config);
    }

    @Operation(summary = "启用 Webhook")
    @PutMapping("/{id}/enable")
    public WebhookConfigDTO enableWebhook(@PathVariable Long id) {
        WebhookConfig config = webhookGateway.findConfigById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook不存在: " + id));
        config.enable();
        WebhookConfig saved = webhookGateway.saveConfig(config);
        return toDTO(saved);
    }

    @Operation(summary = "停用 Webhook")
    @PutMapping("/{id}/disable")
    public WebhookConfigDTO disableWebhook(@PathVariable Long id) {
        WebhookConfig config = webhookGateway.findConfigById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook不存在: " + id));
        config.disable();
        WebhookConfig saved = webhookGateway.saveConfig(config);
        return toDTO(saved);
    }

    @Operation(summary = "删除 Webhook")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long id) {
        webhookGateway.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "查询 Webhook 推送日志")
    @GetMapping("/{id}/logs")
    public List<WebhookLog> getWebhookLogs(@PathVariable Long id,
                                            @RequestParam(defaultValue = "50") int limit) {
        return webhookGateway.findLogsByWebhookId(id);
    }

    @Operation(summary = "查询最近推送日志")
    @GetMapping("/logs")
    public List<WebhookLog> getRecentLogs(@RequestParam(defaultValue = "100") int limit) {
        return webhookGateway.findRecentLogs(limit);
    }

    private WebhookConfigDTO toDTO(WebhookConfig config) {
        WebhookConfigDTO dto = new WebhookConfigDTO();
        dto.setId(config.getId());
        dto.setWebhookUrl(config.getWebhookUrl());
        dto.setEventTypes(config.getEventTypes());
        dto.setDescription(config.getDescription());
        dto.setEnabled(config.isEnabled());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }
}
