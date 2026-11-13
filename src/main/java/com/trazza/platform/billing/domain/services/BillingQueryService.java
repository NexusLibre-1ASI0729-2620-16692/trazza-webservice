package com.trazza.platform.billing.domain.services;

import com.trazza.platform.billing.domain.model.aggregates.Plan;

import java.util.List;

public interface BillingQueryService {

    List<Plan> listPlans();

    List<SubscriptionView> listMySubscriptions(Long userId);

    List<FreightSettlementView> listMySettlements(Long userId);

    List<CompensationView> listMyCompensations(Long userId);
}
