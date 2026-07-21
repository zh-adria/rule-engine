package com.insurance.ruleengine.infrastructure.crypto;

import com.insurance.ruleengine.infrastructure.config.RuleEngineProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * P0-4: AES encryption round-trip + startup key validation.
 */
class AesCryptoGatewayTest {

    @Test
    void encryptThenDecryptRoundTrips() {
        AesCryptoGateway gateway = gatewayWithKey("MyRealKey-16Chars!!");

        String plain = "package insurance; rule \"x\" when then end";
        String cipher = gateway.encrypt(plain);

        assertNotEquals(plain, cipher);
        assertEquals(plain, gateway.decrypt(cipher));
    }

    @Test
    void validateKeyOnStartup_rejectsBlankKey() {
        AesCryptoGateway gateway = gatewayWithKey("   ");
        assertThrows(IllegalStateException.class, gateway::validateKeyOnStartup);
    }

    @Test
    void validateKeyOnStartup_rejectsNullKey() {
        AesCryptoGateway gateway = gatewayWithKey(null);
        assertThrows(IllegalStateException.class, gateway::validateKeyOnStartup);
    }

    @Test
    void validateKeyOnStartup_rejectsTextbookExampleKey() {
        AesCryptoGateway gateway = gatewayWithKey("0123456789abcdef0123456789abcdef");
        assertThrows(IllegalStateException.class, gateway::validateKeyOnStartup);
    }

    @Test
    void validateKeyOnStartup_rejectsShortKey() {
        AesCryptoGateway gateway = gatewayWithKey("short");
        assertThrows(IllegalStateException.class, gateway::validateKeyOnStartup);
    }

    @Test
    void validateKeyOnStartup_acceptsValid32CharKey() {
        AesCryptoGateway gateway = gatewayWithKey("rule-engine-local-key-2026!!secure-key-32!");
        gateway.validateKeyOnStartup(); // should not throw
    }

    private AesCryptoGateway gatewayWithKey(String key) {
        RuleEngineProperties properties = new RuleEngineProperties();
        properties.setEncryptionKey(key);
        return new AesCryptoGateway(properties);
    }
}
