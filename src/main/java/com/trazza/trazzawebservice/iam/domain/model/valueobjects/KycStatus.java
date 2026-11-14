package com.trazza.trazzawebservice.iam.domain.model.valueobjects;

/** Identity validation state of a person ({@code person_identities.kyc_status}). */
public enum KycStatus {
    PENDING,
    VALIDATED,
    REJECTED
}
