package com.trazza.platform.execution.domain.services;

import com.trazza.platform.execution.domain.model.aggregates.DeviationAlert;
import com.trazza.platform.execution.domain.model.aggregates.TrackingPoint;

public record TrackingResult(TrackingPoint point, DeviationAlert alert, String tripStatus) {
}
