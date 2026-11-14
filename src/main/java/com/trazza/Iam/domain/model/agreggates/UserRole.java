package com.trazza.iam.domain.model.aggregates;

import com.trazza.iam.domain.model.valueobjects.UserRoleType;
import java.time.LocalDateTime;

/**
 * Role granted to an account (user_roles, composite key user_id + role).
 */
public class UserRole {
    private Long userId;
    private UserRoleType role;
    private LocalDateTime grantedAt;

    protected UserRole() {}

    public UserRole(UserRoleType role, LocalDateTime grantedAt) {
        this.role = role;
        this.grantedAt = grantedAt;
    }

    // Getters
    public Long getUserId() { return userId; }
    public UserRoleType getRole() { return role; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
}