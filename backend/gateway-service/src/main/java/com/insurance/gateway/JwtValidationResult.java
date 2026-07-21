package com.insurance.gateway;

import java.util.Collections;
import java.util.List;

public class JwtValidationResult {
    private final String username;
    private final List<String> roles;
    private final List<String> permissions;
    private final String tenantCode;

    public JwtValidationResult(String username, List<String> roles, List<String> permissions, String tenantCode) {
        this.username = username;
        this.roles = roles == null ? Collections.emptyList() : roles;
        this.permissions = permissions == null ? Collections.emptyList() : permissions;
        this.tenantCode = tenantCode;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public String getTenantCode() {
        return tenantCode;
    }
}
