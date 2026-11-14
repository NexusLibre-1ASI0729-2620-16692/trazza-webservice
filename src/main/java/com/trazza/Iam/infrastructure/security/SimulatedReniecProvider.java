package com.trazza.iam.infrastructure.security;

import com.trazza.iam.application.internal.outboundservices.IdentityVerificationProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Simulated RENIEC check for the MVP: every DNI is validated except the test DNI 00000000.
 */
@Service
public class SimulatedReniecProvider implements IdentityVerificationProvider {

    public static final String REJECTED_TEST_DNI = "00000000";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public CompletableFuture<ReniecVerificationResult> verify(String dni, String fullName) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String last3 = dni.length() >= 3 ? dni.substring(dni.length() - 3) : dni;
        String reference = String.format("RENIEC-SIM-%s-%s", timestamp, last3);

        if (REJECTED_TEST_DNI.equals(dni)) {
            return CompletableFuture.completedFuture(
                    new ReniecVerificationResult(false, reference, "dni-not-found-in-reniec")
            );
        }

        return CompletableFuture.completedFuture(
                new ReniecVerificationResult(true, reference, null)
        );
    }

    // Record para reemplazar la Tupla de C# (bool Approved, string Reference, string? Reason)
    public record ReniecVerificationResult(
            boolean approved,
            String reference,
            String reason
    ) {}
}