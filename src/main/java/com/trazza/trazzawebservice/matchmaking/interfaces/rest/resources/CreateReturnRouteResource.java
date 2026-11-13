package com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record CreateReturnRouteResource(UUID vehicleId, Long driverId, PlaceResource origin, PlaceResource destination,
                                        Instant departureAt, Integer maxShipments) {
}
