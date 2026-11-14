package com.trazza.trazzawebservice.execution.domain.model.commands;

import com.trazza.trazzawebservice.execution.domain.model.valueobjects.TrackingSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecordTrackingPointCommand(Long userId, UUID tripId, Double latitude, Double longitude,
                                         BigDecimal speedKmh, TrackingSource source, Instant recordedAt) {
}
