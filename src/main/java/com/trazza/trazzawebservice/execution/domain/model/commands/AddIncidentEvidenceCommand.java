package com.trazza.trazzawebservice.execution.domain.model.commands;

import java.util.UUID;

public record AddIncidentEvidenceCommand(Long userId, UUID tripId, Long incidentId, String s3Key) {
}
