package com.insurance.ruleengine.infrastructure.crypto;

import com.insurance.ruleengine.domain.gateway.CryptoGateway;
import com.insurance.ruleengine.infrastructure.config.RuleEngineProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AesCryptoGateway implements CryptoGateway {
    private final RuleEngineProperties properties;

    public AesCryptoGateway(RuleEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public String encrypt(String plainText) {
        return run(Cipher.ENCRYPT_MODE, plainText);
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            Cipher cipher = newCipher(Cipher.DECRYPT_MODE);
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("decrypt rule content failed", e);
        }
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
        byte[] key = properties.getEncryptionKey().substring(0, 16).getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(mode, keySpec);
        return cipher;
    }
}

