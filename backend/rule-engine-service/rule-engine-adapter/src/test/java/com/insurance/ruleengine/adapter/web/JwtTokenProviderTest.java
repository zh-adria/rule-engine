package com.insurance.ruleengine.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {
    @Test
    void shouldRoundTripUsernameWithJsonSpecialCharacters() {
        JwtTokenProvider provider = newProvider();

        String username = "admin\"quoted\\name";
        String token = provider.generateToken(username);

        assertEquals(username, provider.validateAndGetUsername(token));
    }

    @Test
    void shouldRejectTokenWithTamperedSignature() {
        JwtTokenProvider provider = newProvider();

        String token = provider.generateToken("admin");
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThrows(IllegalArgumentException.class, () -> provider.validateAndGetUsername(tampered));
    }

    private JwtTokenProvider newProvider() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", "test-secret-with-enough-entropy");
        ReflectionTestUtils.setField(provider, "expirationSeconds", 3600L);
        return provider;
    }
}
