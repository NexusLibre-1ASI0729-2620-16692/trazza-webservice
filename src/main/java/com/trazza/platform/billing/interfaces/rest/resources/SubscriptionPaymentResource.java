package com.trazza.platform.billing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;

public record SubscriptionPaymentResource(Long id, BigDecimal amount, String currency, String status,
                                          String gatewayPaymentId, Instant paidAt) {
}
