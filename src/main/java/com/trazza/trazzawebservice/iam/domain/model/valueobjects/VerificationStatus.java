package com.trazza.trazzawebservice.iam.domain.model.valueobjects;

/** State of one identity verification attempt ({@code identity_verifications.status}). */
public enum VerificationStatus {
    SUBMITTED,
    VALIDATED,
    REJECTED
}
