package com.trazza.trazzawebservice.iam.application.internal.outboundservices.tokens;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Access token (JWT) and opaque refresh token generation. */
public interface TokenService {

    IssuedToken generateAccessToken(User user);

    /** Public id of the user of a valid access token. */
    Optional<UUID> validateAccessToken(String token);

    IssuedRefreshToken generateRefreshToken();

    byte[] hashRefreshToken(String token);

    record IssuedToken(String token, Instant expiresAt) {
    }

    record IssuedRefreshToken(String token, byte[] hash, Instant expiresAt) {
    }
}
