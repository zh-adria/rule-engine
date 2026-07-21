package com.insurance.ruleengine.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "rule-engine.auth")
public class AuthUserProperties {
    private String jwtSecretKey = "rule-engine-sa-token-jwt-secret-2026";
    private List<User> users = new ArrayList<>();

    public Optional<User> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        return users.stream()
                .filter(user -> username.equals(user.getUsername()))
                .findFirst();
    }

    public String getJwtSecretKey() { return jwtSecretKey; }
    public void setJwtSecretKey(String jwtSecretKey) { this.jwtSecretKey = jwtSecretKey; }
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users == null ? new ArrayList<>() : users; }

    public static class User {
        private String username;
        private String password;
        private String displayName;
        private List<String> roles = new ArrayList<>();
        private List<String> permissions = new ArrayList<>();
        private String tenantCode;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
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
}
