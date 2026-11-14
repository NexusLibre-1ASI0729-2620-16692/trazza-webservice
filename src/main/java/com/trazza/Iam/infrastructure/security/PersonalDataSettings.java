package com.trazza.iam.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Keys (Base64, 32 bytes each) used to protect personal data. In production they come from environment
 * variables (ideally loaded from AWS KMS / Secrets Manager); they must never be committed.
 */
@Component
@ConfigurationProperties(prefix = "personal-data")
public class PersonalDataSettings {

    private String encryptionKey = "";
    private String hashKey = "";

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }
}