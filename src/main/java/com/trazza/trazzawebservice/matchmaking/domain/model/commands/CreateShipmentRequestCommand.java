package com.trazza.trazzawebservice.matchmaking.domain.model.commands;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateShipmentRequestCommand(Long userId, String description, BigDecimal weightKg, BigDecimal volumeM3,
                                           PlaceData pickup, PlaceData delivery, Instant pickupFrom,
                                           Instant pickupUntil) {
}
