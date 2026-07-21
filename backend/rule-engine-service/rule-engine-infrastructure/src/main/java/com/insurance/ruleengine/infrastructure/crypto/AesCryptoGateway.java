package com.insurance.ruleengine.infrastructure.crypto;

import com.insurance.ruleengine.domain.gateway.CryptoGateway;
import com.insurance.ruleengine.infrastructure.config.RuleEngineProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
public class AesCryptoGateway implements CryptoGateway {
    private static final int[] VALID_KEY_LENGTHS = {16, 24, 32};
    private static final String OBFUSCATED_DEV_KEY = "0123456789abcdef0123456789abcdef";

    private final RuleEngineProperties properties;

    public AesCryptoGateway(RuleEngineProperties properties) {
        this.properties = properties;
    }

    /**
     * P0-4: fail-fast startup if the encryption key is missing, too short, or matches the
     * textbook example. Sensitive rule content must be encrypted at rest with a real key.
     */
    @PostConstruct
    public void validateKeyOnStartup() {
        String key = properties.getEncryptionKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "RULE_ENGINE_AES_KEY is empty. Set a 16/24/32-char key via env var or application.yml.");
        }
        if (OBFUSCATED_DEV_KEY.equals(key)) {
            throw new IllegalStateException(
                    "Refusing to start with the textbook example encryption key. Set a real RULE_ENGINE_AES_KEY.");
        }
        boolean lengthOk = false;
        for (int valid : VALID_KEY_LENGTHS) {
            if (key.length() >= valid) {
                lengthOk = true;
                break;
            }
        }
        if (!lengthOk) {
            throw new IllegalStateException(
                    "RULE_ENGINE_AES_KEY must be at least 16 chars (AES-128). Current length=" + key.length());
        }
    }

    @Override
    public String encrypt(String plainText) {
        return run(Cipher.ENCRYPT_MODE, plainText);
    }

    @Override
    public String decrypt(String cipherText) {
        // P2-3: try current key first, then fall back to history for rotation
        List<String> keys = properties.allDecryptKeys();
        if (keys.isEmpty()) {
            throw new IllegalStateException("no decryption key configured");
        }
        byte[] decoded = Base64.getDecoder().decode(cipherText);
        Exception lastError = null;
        for (String key : keys) {
            try {
                Cipher cipher = newCipher(Cipher.DECRYPT_MODE, key);
                return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw new IllegalStateException("decrypt rule content failed with all configured keys", lastError);
    }

    private String run(int mode, String value) {
        try {
            Cipher cipher = newCipher(mode);
            return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("encrypt rule content failed", e);
        }
    }

    private Cipher newCipher(int mode) throws Exception {
        return newCipher(mode, properties.getEncryptionKey());
    }

    private Cipher newCipher(int mode, String key) throws Exception {
        int keyLen;
        if (key.length() >= 32) keyLen = 32;
        else if (key.length() >= 24) keyLen = 24;
        else keyLen = 16;
        byte[] keyBytes = key.substring(0, keyLen).getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(mode, keySpec);
        return cipher;
    }
}
