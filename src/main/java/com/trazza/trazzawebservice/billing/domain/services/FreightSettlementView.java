package com.trazza.trazzawebservice.billing.domain.services;

import com.trazza.trazzawebservice.billing.domain.model.aggregates.FreightSettlement;
import com.trazza.trazzawebservice.execution.interfaces.acl.TripSnapshot;

public record FreightSettlementView(FreightSettlement settlement, TripSnapshot trip, boolean isCarrier) {
}
