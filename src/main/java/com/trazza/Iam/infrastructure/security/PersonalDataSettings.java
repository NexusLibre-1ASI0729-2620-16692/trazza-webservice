package com.trazza.platform.iam.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Keys (Base64, 32 bytes each) used to protect personal data ({@code trazza.personal-data.*}). In production they come
 * from environment variables; they must never be committed.
 */
@ConfigurationProperties(prefix = "trazza.personal-data")
public record PersonalDataSettings(String encryptionKey, String hashKey) {
}
