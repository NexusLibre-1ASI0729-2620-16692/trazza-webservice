package trazzaplatform.api.iam.application.internal.outboundservices;

import trazzaplatform.api.iam.domain.model.aggregates.User;

import java.time.Instant;
import java.util.UUID;

/// <summary>Access token (JWT) and opaque refresh token generation.</summary>
public interface ITokenService {

    AccessTokenResult generateAccessToken(User user);

    UUID validateAccessToken(String token);

    RefreshTokenResult generateRefreshToken();

    byte[] hashRefreshToken(String token);

    // --- Records para reemplazar las tuplas de C# ---

    record AccessTokenResult(String token, Instant expiresAt) {}

    record RefreshTokenResult(String token, byte[] hash, Instant expiresAt) {}
}