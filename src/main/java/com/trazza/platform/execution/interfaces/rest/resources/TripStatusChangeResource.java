package com.trazza.platform.execution.interfaces.rest.resources;

import java.time.Instant;

public record TripStatusChangeResource(String fromStatus, String toStatus, Instant changedAt) {
}
