package com.trazza.iam.infrastructure.tokens;

import com.trazza.iam.application.internal.outboundservices.TokenService;
import com.trazza.iam.domain.model.aggregates.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Signs short-lived access tokens (HMAC-SHA256) whose subject is the user's public id, and creates
 * random refresh tokens of which only the SHA-256 is stored.
 */
@Service
public class JwtTokenService implements TokenService {

    private final TokenSettings settings;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenService(TokenSettings settings) {
        this.settings = settings;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = settings.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public AccessTokenResult generateAccessToken(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(settings.getAccessTokenMinutes());

        String token = Jwts.builder()
                .subject(user.getPublicId().toString())
                .issuedAt(Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .expiration(Date.from(expiresAt.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .issuer(settings.getIssuer())
                .audience().add(settings.getIssuer()).and()
                .claim("role", user.getRoles().stream().map(r -> r.getRole().name()).toList())
                .signWith(getSigningKey())
                .compact();

        return new AccessTokenResult(token, expiresAt);
    }

    @Override
    public UUID validateAccessToken(String token) {
        if (token == null || token.isBlank()) return null;

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .requireIssuer(settings.getIssuer())
                    .requireAudience(settings.getIssuer())
                    // En jjwt 0.12+, el "ClockSkew" se maneja con allowedClockSkewSeconds
                    .clockSkewSeconds(30)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            return (subject != null && !subject.isBlank()) ? UUID.fromString(subject) : null;

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public RefreshTokenResult generateRefreshToken() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(settings.getRefreshTokenDays());
        return new RefreshTokenResult(token, hashRefreshToken(token), expiresAt);
    }

    @Override
    public byte[] hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    // Records para reemplazar las tuplas de C#
    public record AccessTokenResult(String token, LocalDateTime expiresAt) {}
    public record RefreshTokenResult(String token, byte[] hash, LocalDateTime expiresAt) {}
}