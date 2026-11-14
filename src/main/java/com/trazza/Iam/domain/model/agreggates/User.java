package com.trazza.iam.domain.model.aggregates;

import com.trazza.iam.domain.model.valueobjects.UserRoleType;
import com.trazza.shared.domain.model.entities.IAuditableEntity;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Account aggregate root (users + user_roles). Only the password hash is stored.
 */
public class User implements IAuditableEntity {
    public static final int MAX_FAILED_LOGINS = 5;
    public static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private Long id;
    private UUID publicId;
    private String email;
    private String passwordHash;
    private Boolean isActive;
    private Short failedLoginCount = 0;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private final List<UserRole> roles = new ArrayList<>();

    protected User() {
        this.email = "";
        this.passwordHash = "";
    }

    public User(String email, String passwordHash) {
        this.publicId = UUID.randomUUID(); // Equivalente a Guid.CreateVersion7()
        this.email = email;
        this.passwordHash = passwordHash;
        this.isActive = true;
    }

    public List<UserRole> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public boolean hasRole(UserRoleType role) {
        return roles.stream().anyMatch(userRole -> userRole.getRole() == role);
    }

    public boolean grantRole(UserRoleType role, LocalDateTime now) {
        if (hasRole(role)) {
            return false;
        }
        roles.add(new UserRole(role, now));
        return true;
    }

    public boolean canSignIn() {
        return Boolean.TRUE.equals(isActive) && deletedAt == null;
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void registerFailedLogin(LocalDateTime now) {
        this.failedLoginCount++;
        if (this.failedLoginCount < MAX_FAILED_LOGINS) {
            return;
        }
        this.lockedUntil = now.plus(LOCKOUT_DURATION);
        this.failedLoginCount = 0;
    }

    public void registerSuccessfulLogin(LocalDateTime now) {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = now;
    }

    // Getters
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Boolean getIsActive() { return isActive; }
    public Short getFailedLoginCount() { return failedLoginCount; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    @Override public LocalDateTime getCreatedAt() { return createdAt; }
    @Override public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}