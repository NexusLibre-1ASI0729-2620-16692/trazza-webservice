package com.trazza.platform.execution.domain.model.commands;

import java.util.UUID;

public record ConfirmTripCommand(Long userId, UUID tripId) {
}
