package com.insurance.ruleengine.infrastructure.crypto;

import com.insurance.ruleengine.infrastructure.config.RuleEngineProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * P2-3: key rotation — data encrypted with old key is still decryptable when
 * current key + history are both configured.
 */
class AesKeyRotationTest {

    @Test
    void encryptedWithOldKey_decryptedByNewWithHistory() {
        String oldKey = "OldKey-16CharKey!!";
        String newKey = "NewKey-16CharKey!!";

        RuleEngineProperties oldProps = props(oldKey, "");
        AesCryptoGateway enc = new AesCryptoGateway(oldProps);

        String secret = "package insurance; rule \"x\" when then end";
        String cipher = enc.encrypt(secret);

        // rotate: new key is current, old is in history
        RuleEngineProperties newProps = props(newKey, oldKey);
        AesCryptoGateway dec = new AesCryptoGateway(newProps);
        assertEquals(secret, dec.decrypt(cipher));
    }

    @Test
    void currentKeyTriedFirst() {
        String current = "CurrentKey-16Char";
        String other = "SomeOther-16CharK";

        RuleEngineProperties props = props(current, other);
        AesCryptoGateway gw = new AesCryptoGateway(props);

        String plain = "hello world";
        String cipher = gw.encrypt(plain);
        assertEquals(plain, gw.decrypt(cipher));
    }

    @Test
    void multiKeyHistory() {
        String first = "FirstKey-16Chars!!";
        String second = "Second-16CharKey!";
        String third = "Third-16CharKey!!!";

        AesCryptoGateway enc = new AesCryptoGateway(props(first, ""));
        String cipher = enc.encrypt("rotate-me");

        AesCryptoGateway dec = new AesCryptoGateway(props(third, first + "," + second));
        assertEquals("rotate-me", dec.decrypt(cipher));
    }

    @Test
    void blankKeyRejected() {
        assertThrows(IllegalStateException.class, () -> new AesCryptoGateway(props("  ", "")).validateKeyOnStartup());
    }

    @Test
    void decryptFailsWhenNoKeyConfigured() {
        AesCryptoGateway gw = new AesCryptoGateway(props("", ""));
        assertThrows(IllegalStateException.class, () -> gw.decrypt("dGVzdA=="));
    }

    private RuleEngineProperties props(String current, String history) {
        RuleEngineProperties p = new RuleEngineProperties();
        p.setEncryptionKey(current);
        p.setEncryptionKeyHistory(history);
        return p;
    }
}
