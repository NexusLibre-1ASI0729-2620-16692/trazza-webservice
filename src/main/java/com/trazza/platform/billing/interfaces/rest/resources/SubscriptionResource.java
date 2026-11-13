package com.trazza.platform.billing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SubscriptionResource(Long id, PlanResource plan, String status, Instant currentPeriodStart,
                                   Instant currentPeriodEnd, BigDecimal monthlyAmount,
                                   List<SubscribedVehicleResource> vehicles,
                                   List<SubscriptionPaymentResource> payments, Instant cancelledAt,
                                   Instant createdAt) {
}
