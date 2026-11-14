package com.trazza.trazzawebservice.execution.domain.model.commands;

import java.util.UUID;

public record DeliverTripCommand(Long userId, UUID tripId) {
}
