package com.trazza.iam.domain.model.aggregates;

import java.time.LocalDateTime;

/**
 * Refresh token of a session. Only the SHA-256 of the token is stored (refresh_tokens).
 */
public class RefreshToken {
    private Long id;
    private Long userId;
    private byte[] tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;

    protected RefreshToken() {
        this.tokenHash = new byte[0];
    }

    public RefreshToken(Long userId, byte[] tokenHash, LocalDateTime expiresAt, LocalDateTime now) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = now;
    }

    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(LocalDateTime now) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
        }
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public byte[] getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}