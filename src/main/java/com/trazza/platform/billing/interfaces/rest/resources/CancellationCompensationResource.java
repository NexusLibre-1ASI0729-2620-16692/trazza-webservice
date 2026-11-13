package com.trazza.platform.billing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;

public record CancellationCompensationResource(BillingTripResource trip, String myRole, BigDecimal amount,
                                               String currency, String status, Instant createdAt,
                                               Instant settledAt) {
}
