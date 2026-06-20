package com.insurance.ruleengine.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rule-engine")
public class RuleEngineProperties {
    private String encryptionKey = "0123456789abcdef0123456789abcdef";
    private Execution execution = new Execution();

    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
    public Execution getExecution() { return execution; }
    public void setExecution(Execution execution) { this.execution = execution; }

    public static class Execution {
        private long slowThresholdMs = 10;

        public long getSlowThresholdMs() { return slowThresholdMs; }
        public void setSlowThresholdMs(long slowThresholdMs) { this.slowThresholdMs = slowThresholdMs; }
    }
}

