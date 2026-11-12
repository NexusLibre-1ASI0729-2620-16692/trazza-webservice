package com.trazza.platform.execution.domain.model.aggregates;

import com.trazza.platform.execution.domain.model.entities.IncidentEvidence;
import com.trazza.platform.execution.domain.model.valueobjects.IncidentStatus;
import com.trazza.platform.execution.domain.model.valueobjects.IncidentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Problem reported on a trip (damaged or incomplete cargo, delay...) with photo evidences in S3. */
@Entity
@Table(name = "incidents")
@Getter
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private Long reportedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Column(length = 1000)
    private String resolution;

    @Column(nullable = false)
    private Instant reportedAt;

    private Instant resolvedAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_id", nullable = false)
    @OrderBy("uploadedAt")
    private List<IncidentEvidence> evidences = new ArrayList<>();

    protected Incident() {
    }

    public Incident(Long tripId, Long reportedByUserId, IncidentType type, String description, Instant now) {
        this.tripId = tripId;
        this.reportedByUserId = reportedByUserId;
        this.type = type;
        this.description = description.trim();
        this.status = IncidentStatus.OPEN;
        this.reportedAt = now;
    }

    public List<IncidentEvidence> getEvidences() {
        return Collections.unmodifiableList(evidences);
    }

    public IncidentEvidence addEvidence(String s3Key, Instant now) {
        var evidence = new IncidentEvidence(s3Key.trim(), now);
        evidences.add(evidence);
        if (status == IncidentStatus.OPEN) status = IncidentStatus.IN_REVIEW;
        return evidence;
    }

    public void resolve(String resolutionText, Instant now) {
        status = IncidentStatus.RESOLVED;
        resolution = resolutionText.trim();
        resolvedAt = now;
    }
}
