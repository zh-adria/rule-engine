package com.insurance.approval.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "approval-flow")
public class ApprovalProperties {

    private Callback callback = new Callback();

    public Callback getCallback() { return callback; }
    public void setCallback(Callback callback) { this.callback = callback; }

    public static class Callback {
        private String ruleEngineUrl = "http://localhost:8080/rule-engine";
        private int maxRetries = 3;

        public String getRuleEngineUrl() { return ruleEngineUrl; }
        public void setRuleEngineUrl(String ruleEngineUrl) { this.ruleEngineUrl = ruleEngineUrl; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }
}
