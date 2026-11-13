package com.trazza.platform.execution.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;

public record DeviationAlertResource(Long id, double latitude, double longitude, BigDecimal deviationKm,
                                     Instant detectedAt, Instant acknowledgedAt) {
}
