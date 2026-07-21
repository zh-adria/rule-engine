package com.insurance.ruleengine.domain.gateway;

public interface CryptoGateway {
    String encrypt(String plainText);

    String decrypt(String cipherText);
}

