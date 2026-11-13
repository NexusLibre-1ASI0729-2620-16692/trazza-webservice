package com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateShipmentRequestResource(String description, BigDecimal weightKg, BigDecimal volumeM3,
                                            PlaceResource pickup, PlaceResource delivery, Instant pickupFrom,
                                            Instant pickupUntil) {
}
