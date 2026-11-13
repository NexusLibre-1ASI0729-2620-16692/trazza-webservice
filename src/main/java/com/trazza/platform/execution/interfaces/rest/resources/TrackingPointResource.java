package com.trazza.platform.execution.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;

public record TrackingPointResource(Long id, double latitude, double longitude, BigDecimal speedKmh, String source,
                                    Instant recordedAt) {
}
