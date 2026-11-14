package com.trazza.trazzawebservice.billing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;

public record SubscriptionPaymentResource(Long id, BigDecimal amount, String currency, String status,
                                          String gatewayPaymentId, Instant paidAt) {
}
