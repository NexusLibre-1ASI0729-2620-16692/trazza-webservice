package com.trazza.iam.application.internal.outboundservices;

/**
 * Application-level protection of personal data (AES-256-GCM and HMAC-SHA256).
 */
public interface IPersonalDataProtector {

    byte[] encrypt(String plainText);

    String decrypt(byte[] cipher);

    byte[] hashDni(String dni);
}