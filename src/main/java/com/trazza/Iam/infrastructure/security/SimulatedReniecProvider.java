package com.trazza.platform.iam.infrastructure.security;

import com.trazza.platform.iam.application.internal.outboundservices.security.IdentityVerificationProvider;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Simulated RENIEC check for the MVP: every DNI is validated except the test DNI {@code 00000000}. */
@Service
public class SimulatedReniecProvider implements IdentityVerificationProvider {

    public static final String REJECTED_TEST_DNI = "00000000";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public VerificationOutcome verify(String dni, String fullName) {
        var reference = "RENIEC-SIM-" + ZonedDateTime.now(ZoneOffset.UTC).format(STAMP) + "-"
                + dni.substring(dni.length() - 3);
        return REJECTED_TEST_DNI.equals(dni)
                ? new VerificationOutcome(false, reference, "dni-not-found-in-reniec")
                : new VerificationOutcome(true, reference, null);
    }
}
