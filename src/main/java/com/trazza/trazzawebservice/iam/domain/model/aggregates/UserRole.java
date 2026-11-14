package com.trazza.trazzawebservice.iam.domain.model.aggregates;

import com.trazza.trazzawebservice.iam.domain.model.valueobjects.UserRoleType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

import java.time.Instant;

/** Role granted to an account ({@code user_roles}, composite key user_id + role). */
@Embeddable
@Getter
public class UserRole {

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRoleType role;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    protected UserRole() {
    }

    public UserRole(UserRoleType role, Instant grantedAt) {
        this.role = role;
        this.grantedAt = grantedAt;
    }
}
