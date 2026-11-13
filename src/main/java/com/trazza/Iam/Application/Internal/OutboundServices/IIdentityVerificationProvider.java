package trazzaplatform.api.iam.application.internal.outboundservices;

import java.util.concurrent.CompletableFuture;

/// <summary>Identity provider (RENIEC). The MVP uses a simulated provider.</summary>
public interface IIdentityVerificationProvider {

    CompletableFuture<VerificationResult> verifyAsync(String dni, String fullName);

    // --- Record para reemplazar la tupla de C# ---
    record VerificationResult(boolean approved, String reference, String reason) {}
}