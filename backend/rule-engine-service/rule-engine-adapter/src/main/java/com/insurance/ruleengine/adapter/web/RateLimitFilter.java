package com.insurance.ruleengine.adapter.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * P2-4: simple per-IP rate limiter (fixed-window counter). Disabled when rule-engine.rate-limit.enabled=false.
 * For production, replace with a Redis-backed bucket4j/resilience4j filter.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // after TraceIdFilter but before any controller
public class RateLimitFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int limit;
    private final long windowMs;
    private final boolean enabled;
    private final Map<String, Window> ipWindows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${rule-engine.rate-limit.enabled:false}") boolean enabled,
                           @Value("${rule-engine.rate-limit.default-limit:100}") int limit,
                           @Value("${rule-engine.rate-limit.window-seconds:60}") long windowSeconds) {
        this.enabled = enabled;
        this.limit = limit;
        this.windowMs = windowSeconds * 1000;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String ip = clientIp(httpReq);
        long now = System.currentTimeMillis();
        Window w = ipWindows.computeIfAbsent(ip, k -> new Window(now));
        synchronized (w) {
            if (now - w.start > windowMs) {
                w.start = now;
                w.count.set(0);
            }
            int current = w.count.incrementAndGet();
            if (current > limit) {
                if (response instanceof HttpServletResponse) {
                    HttpServletResponse httpResp = (HttpServletResponse) response;
                    httpResp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    httpResp.getWriter().write("{\"error\":\"rate limit exceeded\"}");
                    log.warn("rate limit exceeded for ip={} count={}/{}", ip, current, limit);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static class Window {
        volatile long start;
        AtomicInteger count = new AtomicInteger(0);

        Window(long start) {
            this.start = start;
        }
    }
}
