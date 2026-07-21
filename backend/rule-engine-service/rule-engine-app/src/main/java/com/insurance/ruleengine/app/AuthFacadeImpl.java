package com.insurance.ruleengine.app;

import cn.dev33.satoken.jwt.SaJwtUtil;
import cn.hutool.json.JSONObject;
import com.insurance.ruleengine.client.api.AuthFacade;
import com.insurance.ruleengine.client.dto.AuthLoginCmd;
import com.insurance.ruleengine.client.dto.AuthSessionDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthFacadeImpl implements AuthFacade {
    private static final String LOGIN_TYPE = "login";
    private static final String DEVICE = "default";
    private static final long TOKEN_TIMEOUT_SECONDS = 86_400L;
    private final AuthUserProperties authUsers;

    public AuthFacadeImpl(AuthUserProperties authUsers) {
        this.authUsers = authUsers;
    }

    @Override
    public AuthSessionDTO login(AuthLoginCmd cmd) {
        AuthUserProperties.User user = authUsers.findByUsername(cmd.getUsername())
                .filter(candidate -> candidate.getPassword().equals(cmd.getPassword()))
                .orElseThrow(() -> new IllegalArgumentException("invalid username or password"));
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("displayName", user.getDisplayName());
        extra.put("roles", user.getRoles());
        extra.put("permissions", user.getPermissions());
        extra.put("tenantCode", user.getTenantCode());
        String token = SaJwtUtil.createToken(
                LOGIN_TYPE, user.getUsername(), DEVICE, TOKEN_TIMEOUT_SECONDS, extra, authUsers.getJwtSecretKey());
        return toSession(user, token);
    }

    @Override
    public AuthSessionDTO current(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("token is required");
        }
        JSONObject payload = SaJwtUtil.getPayloads(token, LOGIN_TYPE, authUsers.getJwtSecretKey());
        AuthSessionDTO session = new AuthSessionDTO();
        session.setToken(token);
        session.setUsername(String.valueOf(payload.get(SaJwtUtil.LOGIN_ID)));
        session.setDisplayName(payload.getStr("displayName", session.getUsername()));
        session.setRoles(readList(payload.get("roles")));
        session.setPermissions(readList(payload.get("permissions")));
        session.setTenantCode(payload.getStr("tenantCode", ""));
        return session;
    }

    @Override
    public void logout(String token) {
        // JWT mode is stateless; clients discard the token. Token revocation can be added with a denylist later.
    }

    private static AuthSessionDTO toSession(AuthUserProperties.User user, String token) {
        AuthSessionDTO session = new AuthSessionDTO();
        session.setToken(token);
        session.setUsername(user.getUsername());
        session.setDisplayName(StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getUsername());
        session.setRoles(user.getRoles());
        session.setPermissions(user.getPermissions());
        session.setTenantCode(user.getTenantCode());
        return session;
    }

    private static List<String> readList(Object value) {
        if (value instanceof List<?>) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        return List.of();
    }
}
