package com.insurance.ruleengine.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ApprovalCallbackSecretInterceptor implements HandlerInterceptor {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String HEADER = "X-Approval-Callback-Secret";

    private final boolean enabled;
    private final String secret;

    public ApprovalCallbackSecretInterceptor(
            @Value("${rule-engine.approval-callback-secret-enabled:true}") boolean enabled,
            @Value("${rule-engine.approval-callback-secret:}") String secret) {
        this.enabled = enabled;
        this.secret = secret == null ? "" : secret;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!isCallbackPath(request.getRequestURI()) || !enabled) {
            return true;
        }
        String provided = request.getHeader(HEADER);
        if (secret.isBlank() || provided == null || !constantTimeEquals(secret, provided)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid approval callback secret\"}");
            return false;
        }
        return true;
    }

    private boolean isCallbackPath(String path) {
        return PATH_MATCHER.match("/api/v1/rules/*/versions/*/approval-callback", path);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
