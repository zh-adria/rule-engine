package com.insurance.approval.infrastructure.flow;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 (approval tests): verify CallbackGatewayImpl posts callback + retries on failure.
 */
class CallbackGatewayImplTest {

    private MockWebServer mockServer;

    @AfterEach
    void tearDown() throws IOException {
        if (mockServer != null) mockServer.shutdown();
    }

    @Test
    void notifyApprovalResult_postsToCorrectUrl_withBody() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        // success on first try
        mockServer.enqueue(new MockResponse().setResponseCode(200));

        String baseUrl = mockServer.url("/rule-engine").toString();
        System.setProperty("approval-flow.callback.rule-engine-url", baseUrl);
        System.setProperty("approval-flow.callback.max-retries", "3");

        CallbackGatewayImpl gateway = new CallbackGatewayImpl();
        // reflectively set fields since @Value only works in Spring context
        inject(gateway, "ruleEngineUrl", baseUrl);
        inject(gateway, "maxRetries", 3);
        inject(gateway, "callbackSecret", "callback-secret");

        gateway.notifyApprovalResult("RULE_VERSION", "RULE_001:2", "APPROVED", "bob", "ok");

        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        // Path may be URL-encoded; decode before assertion
        String path = java.net.URLDecoder.decode(req.getPath(), "UTF-8");
        assertTrue(path.contains("/api/v1/rules/RULE_001/versions/2/approval-callback"), "actual path=" + path);
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("\"status\":\"APPROVED\""), "actual body=" + body);
        assertTrue(body.contains("\"reviewedBy\":\"bob\""), "actual body=" + body);
        assertEquals("callback-secret", req.getHeader("X-Approval-Callback-Secret"));
    }

    @Test
    void notifyApprovalResult_retriesOnFailure_thenSucceeds() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        // First two attempts fail, third succeeds
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse().setResponseCode(200));

        String baseUrl = mockServer.url("/rule-engine").toString();
        CallbackGatewayImpl gateway = new CallbackGatewayImpl();
        inject(gateway, "ruleEngineUrl", baseUrl);
        inject(gateway, "maxRetries", 3);

        gateway.notifyApprovalResult("RULE_VERSION", "R:1", "REJECTED", "alice", "no");

        assertEquals(3, mockServer.getRequestCount());
    }

    @Test
    void notifyApprovalResult_skipsNonRuleVersion() {
        CallbackGatewayImpl gateway = new CallbackGatewayImpl();
        // Should not throw; no server interaction expected.
        gateway.notifyApprovalResult("SOMETHING_ELSE", "x:y", "APPROVED", "a", "b");
    }

    private static void inject(Object target, String field, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
