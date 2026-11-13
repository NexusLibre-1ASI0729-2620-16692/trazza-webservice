package com.trazza.platform.billing.domain.services;

import com.trazza.platform.billing.domain.model.aggregates.CancellationCompensation;
import com.trazza.platform.execution.interfaces.acl.TripSnapshot;

public record CompensationView(CancellationCompensation compensation, TripSnapshot trip, boolean isCarrier) {
}
