package com.insurance.ruleengine.client.dto;

import java.util.ArrayList;
import java.util.List;

public class AuthSessionDTO {
    private String token;
    private String username;
    private String displayName;
    private List<String> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    private String tenantCode;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles == null ? new ArrayList<>() : roles; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions == null ? new ArrayList<>() : permissions;
    }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
}
