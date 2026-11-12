package com.trazza.platform.execution.interfaces.acl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Trip as seen by Billing and Reputation. */
public record TripSnapshot(Long id, UUID publicId, String status, BigDecimal agreedRate, String currency,
                           Long carrierUserId, Long merchantUserId, Instant deliveredAt, Instant confirmedAt,
                           Instant cancelledAt, String originAddress, String destinationAddress) {
}
