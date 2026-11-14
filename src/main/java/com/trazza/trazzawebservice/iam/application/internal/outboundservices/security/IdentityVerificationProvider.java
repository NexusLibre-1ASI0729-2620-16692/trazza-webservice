package com.trazza.trazzawebservice.iam.application.internal.outboundservices.security;

/** Identity provider (RENIEC). The MVP uses a simulated provider. */
public interface IdentityVerificationProvider {

    VerificationOutcome verify(String dni, String fullName);

    record VerificationOutcome(boolean approved, String reference, String reason) {
    }
}
