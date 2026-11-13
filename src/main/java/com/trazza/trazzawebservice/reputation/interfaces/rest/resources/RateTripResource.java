package com.trazza.trazzawebservice.reputation.interfaces.rest.resources;

import java.util.UUID;

public record RateTripResource(UUID tripId, Integer score, String comment) {
}