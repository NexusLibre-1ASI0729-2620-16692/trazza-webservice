package com.trazza.trazzawebservice.execution.domain.model.commands;

import com.trazza.trazzawebservice.execution.domain.model.valueobjects.IncidentType;

import java.util.UUID;

public record ReportIncidentCommand(Long userId, UUID tripId, IncidentType type, String description) {
}
