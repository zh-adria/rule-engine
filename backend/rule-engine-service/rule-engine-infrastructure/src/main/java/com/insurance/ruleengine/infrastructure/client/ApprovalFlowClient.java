package com.insurance.ruleengine.infrastructure.client;

import com.insurance.ruleengine.domain.gateway.ApprovalFlowGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ApprovalFlowClient implements ApprovalFlowGateway {

    private static final Logger log = LoggerFactory.getLogger(ApprovalFlowClient.class);

    private final RestTemplate restTemplate;

    @Value("${rule-engine.approval-flow-url:http://localhost:8082/approval}")
    private String approvalFlowUrl;

    public ApprovalFlowClient() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void validate() {
        String normalized = approvalFlowUrl.replaceAll("/+$", "");
        if (!normalized.contains("/approval")) {
            log.warn("approval-flow-url '{}' missing context path '/approval'. "
                    + "Set rule-engine.approval-flow-url to full base URL, e.g. http://localhost:8082/approval",
                    approvalFlowUrl);
        }
    }

    private String buildUrl(String path) {
        String base = approvalFlowUrl.replaceAll("/+$", "");
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    @Override
    public void submitApproval(String targetType, String targetId, String submittedBy, String reason) {
        String url = buildUrl("/api/v1/approvals");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("targetType", targetType);
        body.put("targetId", targetId);
        body.put("submittedBy", submittedBy);
        body.put("reason", reason);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(url, request, Void.class);
            log.info("Submitted approval for {} {} by {}", targetType, targetId, submittedBy);
        } catch (Exception e) {
            log.error("Failed to submit approval for {} {}: {}", targetType, targetId, e.getMessage());
            throw new RuntimeException("提交审批失败: " + e.getMessage(), e);
        }
    }
}
