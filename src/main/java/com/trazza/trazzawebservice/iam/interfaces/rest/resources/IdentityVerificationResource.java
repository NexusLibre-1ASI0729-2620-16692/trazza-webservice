package com.trazza.trazzawebservice.iam.interfaces.rest.resources;

import java.time.Instant;

public record IdentityVerificationResource(Long id, String provider, String status, String externalReference,
                                           String rejectionReason, Instant submittedAt, Instant reviewedAt) {
}
