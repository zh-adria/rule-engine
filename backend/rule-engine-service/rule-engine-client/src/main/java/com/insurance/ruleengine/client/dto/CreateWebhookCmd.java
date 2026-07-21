package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class CreateWebhookCmd {

    @NotBlank(message = "Webhook URL不能为空")
    private String webhookUrl;

    @NotEmpty(message = "事件类型不能为空")
    private List<String> eventTypes;

    private String secret;

    private String description;

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public List<String> getEventTypes() { return eventTypes; }
    public void setEventTypes(List<String> eventTypes) { this.eventTypes = eventTypes; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
