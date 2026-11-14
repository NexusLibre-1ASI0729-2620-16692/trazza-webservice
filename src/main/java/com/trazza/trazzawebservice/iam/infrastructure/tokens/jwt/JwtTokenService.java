package com.trazza.trazzawebservice.iam.infrastructure.tokens.jwt;

import com.trazza.trazzawebservice.iam.application.internal.outboundservices.tokens.TokenService;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Signs short-lived access tokens (HMAC-SHA256) whose subject is the user's public id, and creates random refresh
 * tokens of which only the SHA-256 is stored.
 */
@Service
public class JwtTokenService implements TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final TokenSettings settings;
    private final SecretKey signingKey;

    public JwtTokenService(TokenSettings settings) {
        if (settings.secret() == null || settings.secret().getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException(
                    "trazza.token.secret must be configured (TOKEN_SECRET) with at least 32 characters.");
        this.settings = settings;
        this.signingKey = Keys.hmacShaKeyFor(settings.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IssuedToken generateAccessToken(User user) {
        var now = Instant.now();
        var expiresAt = now.plus(Duration.ofMinutes(settings.accessTokenMinutes()));
        var token = Jwts.builder()
                .subject(user.getPublicId().toString())
                .claim("role", user.roleTypes().stream().map(Enum::name).toList())
                .issuer(settings.issuer())
                .audience().add(settings.issuer()).and()
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    @Override
    public Optional<UUID> validateAccessToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            var claims = Jwts.parser().verifyWith(signingKey)
                    .requireIssuer(settings.issuer())
                    .requireAudience(settings.issuer())
                    .clockSkewSeconds(30)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public IssuedRefreshToken generateRefreshToken() {
        var bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var expiresAt = Instant.now().plus(Duration.ofDays(settings.refreshTokenDays()));
        return new IssuedRefreshToken(token, hashRefreshToken(token), expiresAt);
    }

    @Override
    public byte[] hashRefreshToken(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
