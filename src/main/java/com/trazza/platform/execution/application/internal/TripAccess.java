package com.trazza.platform.execution.application.internal;

import com.trazza.platform.execution.domain.model.aggregates.Trip;
import com.trazza.platform.matchmaking.interfaces.acl.TripContextSnapshot;

import java.util.Objects;

/** A trip with its route/shipment context and who takes part in it. */
public record TripAccess(Trip trip, TripContextSnapshot context, Long carrierUserId, Long merchantUserId,
                         Long userId) {

    public boolean isCarrier() {
        return Objects.equals(userId, carrierUserId);
    }

    public boolean isMerchant() {
        return Objects.equals(userId, merchantUserId);
    }
}
