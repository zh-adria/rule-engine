package com.insurance.ruleengine.adapter.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> JWT_PAYLOAD_TYPE = new TypeReference<>() {
    };

    @Value("${rule-engine.jwt.secret}")
    private String secret;

    @Value("${rule-engine.jwt.expiration-seconds}")
    private long expirationSeconds;

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        Map<String, Object> payloadClaims = new LinkedHashMap<>();
        payloadClaims.put("sub", username);
        payloadClaims.put("iat", now.getEpochSecond());
        payloadClaims.put("exp", expiry.getEpochSecond());
        String payload = base64Url(toJson(payloadClaims));
        String content = header + "." + payload;
        String signature = hmacSha256(content, secret);

        return content + "." + signature;
    }

    public String validateAndGetUsername(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String content = parts[0] + "." + parts[1];
        String expectedSig = hmacSha256(content, secret);
        if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }

        Map<String, Object> payload = parsePayload(parts[1]);
        long exp = readLongClaim(payload, "exp");
        if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
            throw new IllegalArgumentException("JWT token expired");
        }

        Object subject = payload.get("sub");
        if (!(subject instanceof String) || ((String) subject).isBlank()) {
            throw new IllegalArgumentException("JWT subject is missing");
        }
        return (String) subject;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String base64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private String toJson(Map<String, Object> value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JWT payload", e);
        }
    }

    private Map<String, Object> parsePayload(String encodedPayload) {
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(payloadJson, JWT_PAYLOAD_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT payload", e);
        }
    }

    private long readLongClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalArgumentException("JWT claim is missing or invalid: " + name);
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }
}
