package com.insurance.approval.adapter;

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
public class ApprovalPermissionInterceptor implements HandlerInterceptor {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String required = requiredPermission(request.getMethod(), request.getRequestURI());
        if (required == null) {
            return true;
        }
        Set<String> permissions = readPermissions(request.getHeader("X-Auth-Permissions"));
        if (permissions.contains(required) || permissions.contains("ADMIN")) {
            return true;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Missing permission: " + required + "\"}");
        return false;
    }

    private String requiredPermission(String method, String path) {
        if (!PATH_MATCHER.match("/api/v1/approvals/**", path)) {
            return null;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return "APPROVAL_READ";
        }
        if (PATH_MATCHER.match("/api/v1/approvals/*/approve", path)
                || PATH_MATCHER.match("/api/v1/approvals/*/reject", path)) {
            return "RULE_APPROVE";
        }
        if ("POST".equalsIgnoreCase(method)) {
            return "RULE_UPDATE";
        }
        return "APPROVAL_READ";
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
