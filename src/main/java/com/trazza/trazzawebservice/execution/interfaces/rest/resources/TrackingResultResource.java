package com.trazza.trazzawebservice.execution.interfaces.rest.resources;

public record TrackingResultResource(TrackingPointResource point, DeviationAlertResource alert, String tripStatus) {
}
