package com.trazza.platform.billing.domain.services;

import com.trazza.platform.billing.domain.model.aggregates.Plan;
import com.trazza.platform.billing.domain.model.aggregates.Subscription;
import com.trazza.platform.billing.domain.model.aggregates.SubscriptionPayment;

import java.util.List;

public record SubscriptionView(Subscription subscription, Plan plan, List<SubscribedVehicle> vehicles,
                               List<SubscriptionPayment> payments) {
}
