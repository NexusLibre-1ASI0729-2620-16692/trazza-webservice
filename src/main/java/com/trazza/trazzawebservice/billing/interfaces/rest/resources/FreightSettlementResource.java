package com.trazza.trazzawebservice.billing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;

public record FreightSettlementResource(BillingTripResource trip, String myRole, BigDecimal amount, String currency,
                                        String status, Instant merchantMarkedPaidAt, Instant carrierConfirmedAt,
                                        String externalReference) {
}
