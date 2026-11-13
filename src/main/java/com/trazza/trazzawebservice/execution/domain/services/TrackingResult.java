package com.trazza.trazzawebservice.execution.domain.services;

import com.trazza.trazzawebservice.execution.domain.model.aggregates.DeviationAlert;
import com.trazza.trazzawebservice.execution.domain.model.aggregates.TrackingPoint;

public record TrackingResult(TrackingPoint point, DeviationAlert alert, String tripStatus) {
}
