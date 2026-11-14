package com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShipmentRequestResource(UUID id, String description, BigDecimal weightKg, BigDecimal volumeM3,
                                      PlaceResource pickup, PlaceResource delivery, Instant pickupFrom,
                                      Instant pickupUntil, String status, String merchantName, int pendingProposals,
                                      String cancellationReason, Instant cancelledAt, Instant createdAt) {
}
