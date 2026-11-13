package com.trazza.iam.application.internal.outboundservices;

import com.trazza.iam.domain.model.aggregates.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Access token (JWT) and opaque refresh token generation.
 */
public interface ITokenService {

    AccessTokenResult generateAccessToken(User user);

    UUID validateAccessToken(String token);

    RefreshTokenResult generateRefreshToken();

    byte[] hashRefreshToken(String token);

    // --- Records para reemplazar las tuplas de C# ---

    record AccessTokenResult(String token, LocalDateTime expiresAt) {}

    record RefreshTokenResult(String token, byte[] hash, LocalDateTime expiresAt) {}
}