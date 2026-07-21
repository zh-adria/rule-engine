package com.insurance.approval.infrastructure.flow;

import com.insurance.approval.domain.gateway.CallbackGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CallbackGatewayImpl implements CallbackGateway {

    private static final Logger log = LoggerFactory.getLogger(CallbackGatewayImpl.class);

    private final RestTemplate restTemplate;

    @Value("${approval-flow.callback.rule-engine-url:http://localhost:8080/rule-engine}")
    private String ruleEngineUrl;

    @Value("${approval-flow.callback.max-retries:3}")
    private int maxRetries;

    @Value("${approval-flow.callback.secret:}")
    private String callbackSecret;

    public CallbackGatewayImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void notifyApprovalResult(String targetType, String targetId,
                                     String status, String reviewedBy, String reason) {
        if (!"RULE_VERSION".equals(targetType)) {
            log.info("Skipping callback for targetType={}", targetType);
            return;
        }

        // Parse targetId: expected format "RULE_CODE:VERSION"
        String[] parts = targetId.split(":");
        if (parts.length != 2) {
            log.error("Invalid targetId format: {}, expected RULE_CODE:VERSION", targetId);
            return;
        }
        String ruleCode = parts[0];
        int version = Integer.parseInt(parts[1]);

        String callbackUrl = ruleEngineUrl + "/api/v1/rules/" + ruleCode
                + "/versions/" + version + "/approval-callback";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("reviewedBy", reviewedBy);
        body.put("reason", reason);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (callbackSecret != null && !callbackSecret.isBlank()) {
            headers.set("X-Approval-Callback-Secret", callbackSecret);
        }
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                restTemplate.postForObject(callbackUrl, request, Void.class);
                log.info("Callback succeeded for {} v{} status={}", ruleCode, version, status);
                return;
            } catch (Exception e) {
                log.warn("Callback attempt {}/{} failed for {} v{}: {}",
                        attempt, maxRetries, ruleCode, version, e.getMessage());
                if (attempt == maxRetries) {
                    log.error("Callback failed after {} retries for {} v{}", maxRetries, ruleCode, version);
                }
            }
        }
    }
}
