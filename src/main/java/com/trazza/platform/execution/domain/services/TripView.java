package com.trazza.platform.execution.domain.services;

import com.trazza.platform.execution.domain.model.aggregates.TrackingPoint;
import com.trazza.platform.execution.domain.model.aggregates.Trip;
import com.trazza.platform.matchmaking.interfaces.acl.TripContextSnapshot;

import java.util.UUID;

/** A trip with the route, the shipment, both parties, the truck and its last position. */
public record TripView(Trip trip, TripContextSnapshot context, Long carrierUserId, String carrierName,
                       String carrierPhone, Long merchantUserId, String merchantName, String merchantPhone,
                       UUID vehiclePublicId, String vehiclePlate, String driverName, TrackingPoint lastPoint,
                       int openAlerts, boolean isCarrier) {
}
