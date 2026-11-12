package com.trazza.platform.execution.domain.model.commands;

import java.util.UUID;

public record AssignDriverCommand(Long userId, UUID tripId, Long driverId) {
}
