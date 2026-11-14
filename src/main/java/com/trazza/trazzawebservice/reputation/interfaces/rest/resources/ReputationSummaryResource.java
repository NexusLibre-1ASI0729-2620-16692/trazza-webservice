package com.trazza.trazzawebservice.reputation.interfaces.rest.resources;

import java.util.UUID;

public record ReputationSummaryResource(UUID userId, String name, Double averageScore, int ratingsCount) {
}