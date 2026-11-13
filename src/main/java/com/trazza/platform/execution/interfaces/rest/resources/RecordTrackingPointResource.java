package com.trazza.platform.execution.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;

public record RecordTrackingPointResource(Double latitude, Double longitude, BigDecimal speedKmh, String source,
                                          Instant recordedAt) {
}
