package com.trazza.trazzawebservice.iam.domain.model.aggregates;

import com.trazza.trazzawebservice.iam.domain.model.valueobjects.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/** One KYC attempt against RENIEC (simulated in the MVP). Document photos are optional S3 keys. */
@Entity
@Table(name = "identity_verifications")
@Getter
public class IdentityVerification {

    public static final String SIMULATED_PROVIDER = "RENIEC_SIMULATED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    private String externalReference;
    private String rejectionReason;
    @Column(name = "document_front_s3_key")
    private String documentFrontS3Key;

    @Column(name = "document_back_s3_key")
    private String documentBackS3Key;

    @Column(nullable = false)
    private Instant submittedAt;

    private Instant reviewedAt;

    protected IdentityVerification() {
    }

    public IdentityVerification(Long userId, String documentFrontS3Key, String documentBackS3Key, Instant now) {
        this.userId = userId;
        this.provider = SIMULATED_PROVIDER;
        this.status = VerificationStatus.SUBMITTED;
        this.documentFrontS3Key = documentFrontS3Key;
        this.documentBackS3Key = documentBackS3Key;
        this.submittedAt = now;
    }

    public void validate(String externalReference, Instant now) {
        this.status = VerificationStatus.VALIDATED;
        this.externalReference = externalReference;
        this.reviewedAt = now;
    }

    public void reject(String reason, Instant now) {
        this.status = VerificationStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedAt = now;
    }
}
