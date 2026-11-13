package com.trazza.platform.execution.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TripResource(UUID id, String status, String myRole, BigDecimal agreedRate, String currency,
                           UUID matchProposalId, UUID returnRouteId, UUID shipmentRequestId, String description,
                           BigDecimal weightKg, BigDecimal volumeM3, TripPlaceResource origin,
                           TripPlaceResource destination, TripPlaceResource pickup, TripPlaceResource delivery,
                           Instant departureAt, TripPartyResource carrier, TripPartyResource merchant,
                           TripVehicleResource vehicle, Long driverId, String driverName,
                           TrackingPointResource lastPoint, int openAlerts, BigDecimal totalDistanceKm,
                           Instant pickedUpAt, Instant deliveredAt, Instant confirmedAt, Instant cancelledAt,
                           String cancellationReason, Instant createdAt) {
}
