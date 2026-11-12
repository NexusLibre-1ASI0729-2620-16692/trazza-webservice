package com.trazza.platform.execution.domain.model.entities;

import com.trazza.platform.execution.domain.model.valueobjects.TripStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/** Row of {@code trip_status_history}. */
@Entity
@Table(name = "trip_status_history")
@Getter
public class TripStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String fromStatus;

    @Column(nullable = false, length = 20)
    private String toStatus;

    private Long changedByUserId;

    @Column(nullable = false)
    private Instant changedAt;

    protected TripStatusChange() {
    }

    public TripStatusChange(TripStatus fromStatus, TripStatus toStatus, Long changedByUserId, Instant changedAt) {
        this.fromStatus = fromStatus == null ? null : fromStatus.name();
        this.toStatus = toStatus.name();
        this.changedByUserId = changedByUserId;
        this.changedAt = changedAt;
    }
}
