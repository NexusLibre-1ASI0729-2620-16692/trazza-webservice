package com.trazza.trazzawebservice.execution.domain.model.commands;

import java.util.UUID;

public record AcknowledgeDeviationAlertCommand(Long userId, UUID tripId, Long alertId) {
}
