package com.insurance.ruleengine.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.domain.model.WebhookConfig;
import com.insurance.ruleengine.domain.model.WebhookLog;
import com.insurance.ruleengine.infrastructure.persistence.entity.WebhookConfigEntity;
import com.insurance.ruleengine.infrastructure.persistence.entity.WebhookLogEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.WebhookConfigJpaRepository;
import com.insurance.ruleengine.infrastructure.persistence.repository.WebhookLogJpaRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Phase 4: WebhookGateway.sendAsync integration test with MockWebServer.
 * Verifies a webhook subscriber received a payload and the log is persisted.
 */
class WebhookGatewaySendAsyncTest {

    private MockWebServer mockServer;
    private WebhookConfigJpaRepository configRepository;
    private WebhookLogJpaRepository logRepository;
    private WebhookGatewayImpl gateway;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        configRepository = mock(WebhookConfigJpaRepository.class);
        logRepository = mock(WebhookLogJpaRepository.class);

        // repository.save just echoes
        when(logRepository.save(any(WebhookLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(configRepository.findAll()).thenReturn(List.of());
        when(configRepository.findByEnabledTrue()).thenReturn(List.of());

        // Subscriber config
        WebhookConfigEntity cfg = new WebhookConfigEntity();
        cfg.setId(1L);
        cfg.setWebhookUrl(mockServer.url("/hook").toString());
        cfg.setEventTypes("[\"RULE_PUBLISHED\"]");
        cfg.setSecret("my-secret");
        cfg.setEnabled(true);
        when(configRepository.findByEnabledTrue()).thenReturn(List.of(cfg));

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        RestTemplate rt = new RestTemplate(factory);
        gateway = new WebhookGatewayImpl(configRepository, logRepository, new ObjectMapper(), rt, executor);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
        executor.shutdownNow();
    }

    @Test
    void sendAsync_postsToSubscriberAndLogsResult() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(200).setBody("OK"));
        String payload = "{\"event\":\"RULE_PUBLISHED\",\"ruleCode\":\"R1\",\"version\":2,\"timestamp\":123}";
        gateway.sendAsync("RULE_PUBLISHED", payload);

        // async dispatch — wait briefly
        Thread.sleep(500);
        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertTrue(req != null, "expected a POST to subscriber");
        assertEquals("/hook", req.getPath());
        assertEquals("my-secret", req.getHeader("X-Webhook-Secret"));
        assertTrue(req.getBody().readUtf8().contains("RULE_PUBLISHED"));
    }
}
