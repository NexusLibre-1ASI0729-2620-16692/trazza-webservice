package com.trazza.platform.execution.domain.model.commands;

import java.util.UUID;

public record DeliverTripCommand(Long userId, UUID tripId) {
}
