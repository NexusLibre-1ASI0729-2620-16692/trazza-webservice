package com.trazza.platform.execution.domain.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/** Photo of an incident stored in S3 ({@code incident_evidences}). */
@Entity
@Table(name = "incident_evidences")
@Getter
public class IncidentEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "s3_key", nullable = false, length = 512)
    private String s3Key;

    @Column(nullable = false)
    private Instant uploadedAt;

    protected IncidentEvidence() {
    }

    public IncidentEvidence(String s3Key, Instant uploadedAt) {
        this.s3Key = s3Key;
        this.uploadedAt = uploadedAt;
    }
}
