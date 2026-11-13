package com.trazza.platform.billing.domain.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.time.Instant;

@Embeddable
@Getter
public class SubscriptionVehicle {

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected SubscriptionVehicle() {
    }

    public SubscriptionVehicle(Long vehicleId, Instant addedAt) {
        this.vehicleId = vehicleId;
        this.addedAt = addedAt;
    }
}
