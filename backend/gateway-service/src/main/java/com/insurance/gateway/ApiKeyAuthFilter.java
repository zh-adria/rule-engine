package com.insurance.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * P3-external: API Key authentication filter for service-to-service calls.
 * Validates X-Api-Key header against configured keys with scope-based access.
 * Runs after JWT filter (order 0) but before routing.
 */
@Component
public class ApiKeyAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Value("${gateway.api-key.enabled:false}")
    private boolean enabled;

    @Value("${gateway.api-key.header:X-Api-Key}")
    private String headerName;

    @Value("${gateway.api-key.keys:}")
    private List<String> apiKeys;

    @Value("${gateway.public-paths:}")
    private List<String> publicPaths;

    private final Map<String, Set<String>> keyScopes = new HashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Skip if already authenticated by JWT (has X-Auth-Username header)
        if (request.getHeaders().containsKey("X-Auth-Username")) {
            return chain.filter(exchange);
        }

        String apiKey = request.getHeaders().getFirst(headerName);
        if (!StringUtils.hasText(apiKey)) {
            return unauthorized(exchange, "Missing API Key");
        }

        ApiKeyInfo keyInfo = validateApiKey(apiKey);
        if (keyInfo == null) {
            log.warn("Invalid API Key from {}: {}", request.getRemoteAddress(), maskKey(apiKey));
            return unauthorized(exchange, "Invalid API Key");
        }

        // Forward identity to downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Auth-Service-Id", keyInfo.serviceId)
                .header("X-Auth-Scopes", String.join(",", keyInfo.scopes))
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -50; // After JWT (-100), before routing
    }

    private boolean isPublicPath(String path) {
        if (publicPaths == null) return false;
        return publicPaths.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private ApiKeyInfo validateApiKey(String key) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            return null;
        }
        for (String configured : apiKeys) {
            String[] parts = configured.split(":", 3);
            if (parts.length >= 2 && parts[1].equals(key)) {
                String serviceId = parts[0];
                Set<String> scopes = parts.length >= 3
                        ? new HashSet<>(Arrays.asList(parts[2].split(",")))
                        : Collections.emptySet();
                return new ApiKeyInfo(serviceId, scopes);
            }
        }
        return null;
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 4) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = "{\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}";
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }

    // Configuration format: serviceId:apiKey:scope1,scope2
    private static class ApiKeyInfo {
        final String serviceId;
        final Set<String> scopes;

        ApiKeyInfo(String serviceId, Set<String> scopes) {
            this.serviceId = serviceId;
            this.scopes = scopes;
        }
    }
}
