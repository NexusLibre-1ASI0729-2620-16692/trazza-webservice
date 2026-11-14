package com.trazza.trazzawebservice.matchmaking.domain.model.commands;

import java.time.Instant;
import java.util.UUID;

public record CreateReturnRouteCommand(Long userId, UUID vehicleId, Long driverId, PlaceData origin,
                                       PlaceData destination, Instant departureAt, int maxShipments) {
}
