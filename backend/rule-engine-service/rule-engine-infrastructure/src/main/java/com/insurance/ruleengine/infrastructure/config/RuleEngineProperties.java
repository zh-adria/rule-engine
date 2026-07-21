package com.insurance.ruleengine.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "rule-engine")
public class RuleEngineProperties {
    private String encryptionKey = "";
    /** P2-3: comma-separated list of previous keys, used to decrypt legacy data during rotation. */
    private String encryptionKeyHistory = "";
    private Execution execution = new Execution();

    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
    public String getEncryptionKeyHistory() { return encryptionKeyHistory; }
    public void setEncryptionKeyHistory(String encryptionKeyHistory) { this.encryptionKeyHistory = encryptionKeyHistory; }
    public Execution getExecution() { return execution; }
    public void setExecution(Execution execution) { this.execution = execution; }

    /** Returns decryption keys ordered newest-first (current first, then history). */
    public List<String> allDecryptKeys() {
        List<String> keys = new ArrayList<>();
        if (encryptionKey != null && !encryptionKey.isBlank()) {
            keys.add(encryptionKey);
        }
        if (encryptionKeyHistory != null && !encryptionKeyHistory.isBlank()) {
            for (String hist : encryptionKeyHistory.split(",")) {
                String trimmed = hist.trim();
                if (!trimmed.isEmpty() && !keys.contains(trimmed)) {
                    keys.add(trimmed);
                }
            }
        }
        return keys;
    }

    public static class Execution {
        private long slowThresholdMs = 10;

        public long getSlowThresholdMs() { return slowThresholdMs; }
        public void setSlowThresholdMs(long slowThresholdMs) { this.slowThresholdMs = slowThresholdMs; }
    }
}
