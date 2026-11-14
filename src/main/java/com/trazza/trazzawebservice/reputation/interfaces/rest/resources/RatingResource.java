package com.trazza.trazzawebservice.reputation.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record RatingResource(Long id, UUID tripId, UUID counterpartId, String counterpartName, int score,
                             String comment, Instant createdAt) {
}