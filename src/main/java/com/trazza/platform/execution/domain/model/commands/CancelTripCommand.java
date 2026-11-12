package com.trazza.platform.execution.domain.model.commands;

import java.util.UUID;

public record CancelTripCommand(Long userId, UUID tripId, String reason) {
}
