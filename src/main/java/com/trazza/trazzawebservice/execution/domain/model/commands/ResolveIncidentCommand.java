package com.trazza.trazzawebservice.execution.domain.model.commands;

import java.util.UUID;

public record ResolveIncidentCommand(Long userId, UUID tripId, Long incidentId, String resolution) {
}
