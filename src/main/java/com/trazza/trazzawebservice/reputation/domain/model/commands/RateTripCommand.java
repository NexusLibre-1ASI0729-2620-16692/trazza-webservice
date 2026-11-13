package com.trazza.trazzawebservice.reputation.domain.model.commands;

import java.util.UUID;

public record RateTripCommand(Long userId, UUID tripId, Integer score, String comment) {
}