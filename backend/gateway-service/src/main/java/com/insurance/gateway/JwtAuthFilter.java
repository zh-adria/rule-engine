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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global Sa-Token authentication filter for Spring Cloud Gateway.
 * Validates Bearer tokens for all requests except public paths.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final SaTokenGatewayTokenValidator tokenValidator;
    private final List<String> publicPaths;

    public JwtAuthFilter(SaTokenGatewayTokenValidator tokenValidator,
                         @Value("${gateway.public-paths:}") List<String> publicPaths) {
        this.tokenValidator = tokenValidator;
        this.publicPaths = publicPaths;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip auth for public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Skip auth for OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
            return chain.filter(exchange);
        }

        // Extract token from Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            JwtValidationResult result = tokenValidator.validate(token);
            // Forward username to downstream services via header
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Auth-Username", result.getUsername())
                    .header("X-Auth-Roles", String.join(",", result.getRoles()))
                    .header("X-Auth-Permissions", String.join(",", result.getPermissions()))
                    .header("X-Tenant-Code", result.getTenantCode())
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.debug("Sa-Token validation failed: {}", e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    @Override
    public int getOrder() {
        return -100; // High priority, runs before other filters
    }

    private boolean isPublicPath(String path) {
        if (publicPaths == null) return false;
        return publicPaths.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = "{\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}";
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }
}
