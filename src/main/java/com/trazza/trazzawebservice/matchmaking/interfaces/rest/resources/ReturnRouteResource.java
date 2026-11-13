package com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReturnRouteResource(UUID id, PlaceResource origin, PlaceResource destination, Instant departureAt,
                                  int maxShipments, String status, RouteVehicleResource vehicle, Long driverId,
                                  String driverName, int acceptedShipments, BigDecimal availableWeightKg,
                                  BigDecimal availableVolumeM3, int pendingProposals, Instant createdAt) {
}
