package com.trazza.trazzawebservice.execution.domain.model.commands;

import java.util.UUID;

public record PickUpTripCommand(Long userId, UUID tripId) {
}
