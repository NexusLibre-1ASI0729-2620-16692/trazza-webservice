package com.trazza.trazzawebservice.execution.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;

public record IncidentResource(Long id, String type, String description, String status, String resolution,
                               Instant reportedAt, Instant resolvedAt, List<IncidentEvidenceResource> evidences) {
}
