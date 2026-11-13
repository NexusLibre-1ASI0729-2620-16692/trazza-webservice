package com.trazza.platform.execution.interfaces.rest.resources;

import java.time.Instant;

public record IncidentEvidenceResource(Long id, String s3Key, Instant uploadedAt) {
}
