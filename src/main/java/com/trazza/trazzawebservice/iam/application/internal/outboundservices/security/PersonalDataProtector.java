package com.trazza.trazzawebservice.iam.application.internal.outboundservices.security;

/** Application-level protection of personal data (AES-256-GCM and HMAC-SHA256). */
public interface PersonalDataProtector {

    byte[] encrypt(String plainText);

    String decrypt(byte[] cipher);

    byte[] hashDni(String dni);
}
