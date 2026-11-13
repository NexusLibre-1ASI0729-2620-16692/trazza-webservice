package com.trazza.trazzawebservice.reputation.domain.services;

import java.util.UUID;

public record ReputationSummary(UUID userPublicId, String name, Double averageScore, int ratingsCount) {
}