package com.trazza.iam.domain.model.aggregates;

import com.trazza.iam.domain.model.valueobjects.VerificationStatus;
import java.time.LocalDateTime;

/**
 * One KYC attempt against RENIEC (simulated in the MVP). Document photos are optional S3 keys.
 */
public class IdentityVerification {
    public static final String SIMULATED_PROVIDER = "RENIEC_SIMULATED";

    private Long id;
    private Long userId;
    private String provider;
    private VerificationStatus status;
    private String externalReference;
    private String rejectionReason;
    private String documentFrontS3Key;
    private String documentBackS3Key;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    protected IdentityVerification() {
        this.provider = SIMULATED_PROVIDER;
    }

    public IdentityVerification(Long userId, String documentFrontS3Key, String documentBackS3Key, LocalDateTime now) {
        this.userId = userId;
        this.provider = SIMULATED_PROVIDER;
        this.status = VerificationStatus.SUBMITTED;
        this.documentFrontS3Key = documentFrontS3Key;
        this.documentBackS3Key = documentBackS3Key;
        this.submittedAt = now;
    }

    public void validate(String externalReference, LocalDateTime now) {
        this.status = VerificationStatus.VALIDATED;
        this.externalReference = externalReference;
        this.reviewedAt = now;
    }

    public void reject(String reason, LocalDateTime now) {
        this.status = VerificationStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedAt = now;
    }

    // Getters...
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getProvider() { return provider; }
    public VerificationStatus getStatus() { return status; }
    public String getExternalReference() { return externalReference; }
    public String getRejectionReason() { return rejectionReason; }
    public String getDocumentFrontS3Key() { return documentFrontS3Key; }
    public String getDocumentBackS3Key() { return documentBackS3Key; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
}