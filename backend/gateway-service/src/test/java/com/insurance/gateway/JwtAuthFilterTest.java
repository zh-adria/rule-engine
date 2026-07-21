package com.insurance.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtAuthFilterTest {

    @Test
    void replacesSpoofedIdentityHeadersWithValidatedClaims() {
        JwtAuthFilter filter = new JwtAuthFilter(new StubTokenValidator(), Collections.emptyList());
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/rule-engine/api/v1/rules")
                .header("Authorization", "Bearer valid-token")
                .header("X-Auth-Username", "mallory")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Auth-Permissions", "RULE_PUBLISH")
                .header("X-Tenant-Code", "spoofed")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, chainCapturing(forwarded)).block();

        assertEquals("alice", forwarded.get().getRequest().getHeaders().getFirst("X-Auth-Username"));
        assertEquals("RULE_AUTHOR", forwarded.get().getRequest().getHeaders().getFirst("X-Auth-Roles"));
        assertEquals("RULE_READ,RULE_CREATE", forwarded.get().getRequest().getHeaders().getFirst("X-Auth-Permissions"));
        assertEquals("tenant-a", forwarded.get().getRequest().getHeaders().getFirst("X-Tenant-Code"));
        assertEquals(Arrays.asList("alice"), forwarded.get().getRequest().getHeaders().get("X-Auth-Username"));
        assertEquals(Arrays.asList("RULE_AUTHOR"), forwarded.get().getRequest().getHeaders().get("X-Auth-Roles"));
        assertEquals(Arrays.asList("RULE_READ,RULE_CREATE"), forwarded.get().getRequest().getHeaders().get("X-Auth-Permissions"));
        assertEquals(Arrays.asList("tenant-a"), forwarded.get().getRequest().getHeaders().get("X-Tenant-Code"));
    }

    private GatewayFilterChain chainCapturing(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private static class StubTokenValidator extends SaTokenGatewayTokenValidator {
        StubTokenValidator() {
            super("test-secret");
        }

        @Override
        public JwtValidationResult validate(String token) {
            return new JwtValidationResult(
                    "alice",
                    Arrays.asList("RULE_AUTHOR"),
                    Arrays.asList("RULE_READ", "RULE_CREATE"),
                    "tenant-a");
        }
    }
}
