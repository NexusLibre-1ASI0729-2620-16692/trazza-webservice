package com.trazza.iam.application.internal.outboundservices;

import java.util.concurrent.CompletableFuture;

/**
 * Identity provider (RENIEC). The MVP uses a simulated provider.
 */
public interface IIdentityVerificationProvider {

    CompletableFuture<VerificationResult> verifyAsync(String dni, String fullName);

    // --- Record para reemplazar la tupla de C# ---
    record VerificationResult(boolean approved, String reference, String reason) {}
}