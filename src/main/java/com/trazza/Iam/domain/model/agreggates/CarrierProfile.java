package com.trazza.iam.domain.model.aggregates;

import com.trazza.shared.domain.model.entities.IAuditableEntity;
import java.time.LocalDateTime;

/**
 * Carrier profile (carrier_profiles): owner of the fleet (vehicles and drivers).
 */
public class CarrierProfile implements IAuditableEntity {
    private Long id;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected CarrierProfile() {}

    public CarrierProfile(Long userId) {
        this.userId = userId;
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    @Override public LocalDateTime getCreatedAt() { return createdAt; }
    @Override public LocalDateTime getUpdatedAt() { return updatedAt; }
}