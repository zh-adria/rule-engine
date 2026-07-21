package com.insurance.ruleengine.domain.model;

import java.time.LocalDateTime;

/**
 * Webhook 调用日志
 */
public class WebhookLog {

    private Long id;
    private Long webhookId;
    private String eventType;
    private String ruleCode;
    private String requestUrl;
    private String requestBody;
    private Integer responseStatus;
    private String responseBody;
    private boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;

    private WebhookLog() {}

    public static WebhookLog create(Long webhookId, String eventType, String ruleCode,
                                     String requestUrl, String requestBody) {
        WebhookLog log = new WebhookLog();
        log.webhookId = webhookId;
        log.eventType = eventType;
        log.ruleCode = ruleCode;
        log.requestUrl = requestUrl;
        log.requestBody = requestBody;
        log.success = false;
        log.createdAt = LocalDateTime.now();
        return log;
    }

    public void markSuccess(Integer responseStatus, String responseBody) {
        this.success = true;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    public void markFailed(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getWebhookId() { return webhookId; }

    public String getEventType() { return eventType; }

    public String getRuleCode() { return ruleCode; }

    public String getRequestUrl() { return requestUrl; }

    public String getRequestBody() { return requestBody; }

    public Integer getResponseStatus() { return responseStatus; }

    public String getResponseBody() { return responseBody; }

    public boolean isSuccess() { return success; }

    public String getErrorMessage() { return errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
