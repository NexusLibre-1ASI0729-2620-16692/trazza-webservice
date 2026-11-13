package com.trazza.platform.execution.interfaces.rest.resources;

public record TrackingResultResource(TrackingPointResource point, DeviationAlertResource alert, String tripStatus) {
}
