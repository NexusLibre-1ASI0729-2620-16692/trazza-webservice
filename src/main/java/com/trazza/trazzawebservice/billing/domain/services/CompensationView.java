package com.trazza.trazzawebservice.billing.domain.services;

import com.trazza.trazzawebservice.billing.domain.model.aggregates.CancellationCompensation;
import com.trazza.trazzawebservice.execution.interfaces.acl.TripSnapshot;

public record CompensationView(CancellationCompensation compensation, TripSnapshot trip, boolean isCarrier) {
}
