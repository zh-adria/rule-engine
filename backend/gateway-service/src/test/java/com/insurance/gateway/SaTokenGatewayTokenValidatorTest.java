package com.insurance.gateway;

import cn.dev33.satoken.jwt.SaJwtUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SaTokenGatewayTokenValidatorTest {

    private static final String SECRET = "rule-engine-sa-token-jwt-secret-2026";

    @Test
    void validatesSaTokenJwtAndExtractsContext() {
        SaTokenGatewayTokenValidator validator = new SaTokenGatewayTokenValidator(SECRET);
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("displayName", "Alice");
        extra.put("roles", Arrays.asList("UNDERWRITER"));
        extra.put("permissions", Arrays.asList("RULE_READ", "RULE_WRITE"));
        extra.put("tenantCode", "tenant-a");

        JwtValidationResult result = validator.validate(
                SaJwtUtil.createToken("login", "alice", "default", 86_400L, extra, SECRET));

        assertEquals("alice", result.getUsername());
        assertEquals(Arrays.asList("UNDERWRITER"), result.getRoles());
        assertEquals(Arrays.asList("RULE_READ", "RULE_WRITE"), result.getPermissions());
        assertEquals("tenant-a", result.getTenantCode());
    }

    @Test
    void rejectsMalformedToken() {
        SaTokenGatewayTokenValidator validator = new SaTokenGatewayTokenValidator(SECRET);

        assertThrows(IllegalArgumentException.class, () -> validator.validate("bad-token"));
    }
}
