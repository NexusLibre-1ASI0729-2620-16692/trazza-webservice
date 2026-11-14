package com.trazza.trazzawebservice.reputation.interfaces.rest.resources;

import java.time.Instant;

public record LoyaltyEntryResource(Long id, int points, String reason, Instant createdAt) {
}