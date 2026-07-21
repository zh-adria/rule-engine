package com.insurance.gateway;

import cn.dev33.satoken.jwt.SaJwtUtil;
import cn.hutool.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SaTokenGatewayTokenValidator {
    private static final String LOGIN_TYPE = "login";
    private final String jwtSecretKey;

    public SaTokenGatewayTokenValidator(@Value("${gateway.auth.jwt-secret-key:}") String jwtSecretKey) {
        if (!StringUtils.hasText(jwtSecretKey)) {
            throw new IllegalArgumentException("gateway.auth.jwt-secret-key is required");
        }
        this.jwtSecretKey = jwtSecretKey;
    }

    public JwtValidationResult validate(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("token is required");
        }
        try {
            JSONObject payload = SaJwtUtil.getPayloads(token, LOGIN_TYPE, jwtSecretKey);
            String username = String.valueOf(payload.get(SaJwtUtil.LOGIN_ID));
            if (!StringUtils.hasText(username) || "null".equals(username)) {
                throw new IllegalArgumentException("Sa-Token login id is missing");
            }
            return new JwtValidationResult(
                    username,
                    readList(payload.get("roles")),
                    readList(payload.get("permissions")),
                    readString(payload.get("tenantCode")));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Sa-Token JWT", e);
        }
    }

    private static String readString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static List<String> readList(Object value) {
        if (value instanceof List<?>) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        if (value == null) {
            return Collections.emptyList();
        }
        String raw = String.valueOf(value);
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}
