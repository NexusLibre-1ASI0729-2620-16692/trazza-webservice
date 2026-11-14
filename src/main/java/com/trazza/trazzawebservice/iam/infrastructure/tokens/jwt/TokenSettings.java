package com.trazza.trazzawebservice.iam.infrastructure.tokens.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings ({@code trazza.token.*}). The secret comes from {@code TOKEN_SECRET} and needs at least 32 characters.
 */
@ConfigurationProperties(prefix = "trazza.token")
public record TokenSettings(String secret, String issuer, Integer accessTokenMinutes, Integer refreshTokenDays) {

    public TokenSettings {
        if (issuer == null || issuer.isBlank()) issuer = "trazza-platform";
        if (accessTokenMinutes == null) accessTokenMinutes = 60;
        if (refreshTokenDays == null) refreshTokenDays = 14;
    }
}
