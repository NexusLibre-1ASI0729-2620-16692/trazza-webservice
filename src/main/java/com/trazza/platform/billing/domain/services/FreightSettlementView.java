package com.trazza.platform.billing.domain.services;

import com.trazza.platform.billing.domain.model.aggregates.FreightSettlement;
import com.trazza.platform.execution.interfaces.acl.TripSnapshot;

public record FreightSettlementView(FreightSettlement settlement, TripSnapshot trip, boolean isCarrier) {
}
