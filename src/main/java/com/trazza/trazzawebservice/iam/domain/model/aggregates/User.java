package com.trazza.trazzawebservice.iam.domain.model.aggregates;

import com.trazza.trazzawebservice.iam.domain.model.valueobjects.UserRoleType;
import com.trazza.trazzawebservice.shared.domain.model.entities.AuditableEntity;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.PublicIds;
import com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa.converters.PublicIdConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Account aggregate root ({@code users} + {@code user_roles}). Only the password hash is stored.
 */
@Entity
@Table(name = "users")
@Getter
public class User extends AuditableEntity {

    public static final int MAX_FAILED_LOGINS = 5;
    public static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PublicIdConverter.class)
    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "binary(16)")
    private UUID publicId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int failedLoginCount;

    private Instant lockedUntil;
    private Instant lastLoginAt;
    private Instant deletedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private List<UserRole> roles = new ArrayList<>();

    protected User() {
    }

    public User(String email, String passwordHash) {
        this.publicId = PublicIds.newId();
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = true;
    }

    public List<UserRole> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public List<UserRoleType> roleTypes() {
        return roles.stream().map(UserRole::getRole).toList();
    }

    public boolean hasRole(UserRoleType role) {
        return roles.stream().anyMatch(userRole -> userRole.getRole() == role);
    }

    public boolean grantRole(UserRoleType role, Instant now) {
        if (hasRole(role)) return false;
        roles.add(new UserRole(role, now));
        return true;
    }

    public boolean canSignIn() {
        return active && deletedAt == null;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** Five wrong passwords in a row lock the account for fifteen minutes. */
    public void registerFailedLogin(Instant now) {
        failedLoginCount++;
        if (failedLoginCount < MAX_FAILED_LOGINS) return;
        lockedUntil = now.plus(LOCKOUT_DURATION);
        failedLoginCount = 0;
    }

    public void registerSuccessfulLogin(Instant now) {
        failedLoginCount = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }
}
