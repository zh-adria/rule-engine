package com.insurance.ruleengine.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class PermissionInterceptor implements HandlerInterceptor {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String PERMISSIONS_HEADER = "X-Auth-Permissions";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isInternalCallback(path)) {
            return true;
        }

        String required = requiredPermission(method, path);
        if (required == null) {
            return true;
        }

        Set<String> permissions = readPermissions(request.getHeader(PERMISSIONS_HEADER));
        if (permissions.contains(required) || permissions.contains("ADMIN")) {
            return true;
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Missing permission: " + required + "\"}");
        return false;
    }

    private boolean isInternalCallback(String path) {
        return PATH_MATCHER.match("/api/v1/rules/*/versions/*/approval-callback", path);
    }

    private String requiredPermission(String method, String path) {
        if (PATH_MATCHER.match("/api/v1/rules/*/audits", path)) return "AUDIT_READ";
        if (PATH_MATCHER.match("/api/v1/metrics/**", path)) return "AUDIT_READ";
        if (PATH_MATCHER.match("/api/v1/rules/*/publish", path)) return "RULE_PUBLISH";
        if (PATH_MATCHER.match("/api/v1/rules/*/rollback", path)) return "RULE_PUBLISH";
        if (PATH_MATCHER.match("/api/v1/rules/*/archive", path)) return "RULE_ARCHIVE";
        if (PATH_MATCHER.match("/api/v1/rules/*/test", path)) return "RULE_TEST";
        if (PATH_MATCHER.match("/api/v1/rules/execute", path)) return "RULE_TEST";
        if (PATH_MATCHER.match("/api/v1/rules/*/versions/*/approve", path)) return "RULE_APPROVE";
        if (PATH_MATCHER.match("/api/v1/rules/*/versions/*/reject", path)) return "RULE_APPROVE";
        if (PATH_MATCHER.match("/api/v1/rules/*/versions/*/submit-approval", path)) return "RULE_UPDATE";
        if (PATH_MATCHER.match("/api/v1/rule-sets/execute", path)) return "RULE_SET_EXECUTE";
        if (PATH_MATCHER.match("/api/v1/rule-sets/**", path)) {
            if ("GET".equalsIgnoreCase(method)) return "RULE_SET_READ";
            if ("POST".equalsIgnoreCase(method)) return "RULE_SET_CREATE";
            if ("PUT".equalsIgnoreCase(method)) return "RULE_SET_UPDATE";
            if ("DELETE".equalsIgnoreCase(method)) return "RULE_SET_DELETE";
        }
        if (PATH_MATCHER.match("/api/v1/custom-fields/**", path)) {
            if ("GET".equalsIgnoreCase(method)) return "RULE_READ";
            return "USER_MANAGE";
        }
        if (PATH_MATCHER.match("/api/v1/templates/**", path)) {
            if ("GET".equalsIgnoreCase(method)) return "RULE_READ";
            return "RULE_UPDATE";
        }
        if (PATH_MATCHER.match("/api/v1/webhooks/**", path)) {
            if ("GET".equalsIgnoreCase(method)) return "WEBHOOK_READ";
            if ("POST".equalsIgnoreCase(method)) return "WEBHOOK_CREATE";
            if ("PUT".equalsIgnoreCase(method)) return "WEBHOOK_UPDATE";
            if ("DELETE".equalsIgnoreCase(method)) return "WEBHOOK_DELETE";
        }
        if (PATH_MATCHER.match("/api/v1/rules/**", path)) {
            if ("GET".equalsIgnoreCase(method)) return "RULE_READ";
            if ("POST".equalsIgnoreCase(method)) {
                return PATH_MATCHER.match("/api/v1/rules", path) ? "RULE_CREATE" : "RULE_UPDATE";
            }
        }
        return null;
    }

    private Set<String> readPermissions(String raw) {
        Set<String> permissions = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return permissions;
        }
        Arrays.stream(raw.split("[,\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .forEach(permissions::add);
        return permissions;
    }
}
