package com.trazza.trazzawebservice.execution.domain.model.aggregates;

import com.trazza.trazzawebservice.execution.domain.model.entities.TripStatusChange;
import com.trazza.trazzawebservice.execution.domain.model.valueobjects.TripStatus;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import com.trazza.trazzawebservice.shared.domain.model.entities.AuditableEntity;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.PublicIds;
import com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa.converters.PublicIdConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Execution of an accepted proposal: SCHEDULED, PICKED_UP, IN_TRANSIT, DELIVERED and CONFIRMED by the merchant, or
 * CANCELLED. Every change is written to {@code trip_status_history}.
 */
@Entity
@Table(name = "trips")
@Getter
public class Trip extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PublicIdConverter.class)
    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "binary(16)")
    private UUID publicId;

    @Column(nullable = false)
    private Long matchProposalId;

    private Long driverId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal agreedRate;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    private Instant pickedUpAt;
    private Instant deliveredAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private Long cancelledByUserId;
    private String cancellationReason;

    @Column(precision = 8, scale = 2)
    private BigDecimal totalDistanceKm;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private List<TripStatusChange> history = new ArrayList<>();

    protected Trip() {
    }

    public Trip(Long matchProposalId, Long driverId, BigDecimal agreedRate, String currency, Long createdByUserId,
                Instant now) {
        this.publicId = PublicIds.newId();
        this.matchProposalId = matchProposalId;
        this.driverId = driverId;
        this.agreedRate = agreedRate;
        this.currency = currency;
        this.status = TripStatus.SCHEDULED;
        history.add(new TripStatusChange(null, status, createdByUserId, now));
    }

    public List<TripStatusChange> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public boolean isClosed() {
        return status == TripStatus.CONFIRMED || status == TripStatus.CANCELLED;
    }

    public boolean isOnTheRoad() {
        return status == TripStatus.PICKED_UP || status == TripStatus.IN_TRANSIT;
    }

    public Result<Trip> assignDriver(Long newDriverId) {
        if (status != TripStatus.SCHEDULED && status != TripStatus.PICKED_UP)
            return Result.failure(CommandError.conflict("trip-status-invalid"));
        driverId = newDriverId;
        return Result.success(this);
    }

    public Result<Trip> pickUp(Long userId, Instant now) {
        if (status != TripStatus.SCHEDULED) return Result.failure(CommandError.conflict("trip-status-invalid"));
        pickedUpAt = now;
        return Result.success(changeStatus(TripStatus.PICKED_UP, userId, now));
    }

    /** The first GPS point after the pick-up moves the trip to IN_TRANSIT. */
    public void registerMovement(double addedDistanceKm, Long userId, Instant now) {
        if (addedDistanceKm > 0) {
            var current = totalDistanceKm == null ? BigDecimal.ZERO : totalDistanceKm;
            totalDistanceKm = current.add(BigDecimal.valueOf(addedDistanceKm)).setScale(2, RoundingMode.HALF_EVEN);
        }
        if (status == TripStatus.PICKED_UP) changeStatus(TripStatus.IN_TRANSIT, userId, now);
    }

    public Result<Trip> deliver(Long userId, Instant now) {
        if (!isOnTheRoad()) return Result.failure(CommandError.conflict("trip-status-invalid"));
        deliveredAt = now;
        return Result.success(changeStatus(TripStatus.DELIVERED, userId, now));
    }

    public Result<Trip> confirm(Long userId, Instant now) {
        if (status != TripStatus.DELIVERED) return Result.failure(CommandError.conflict("trip-status-invalid"));
        confirmedAt = now;
        return Result.success(changeStatus(TripStatus.CONFIRMED, userId, now));
    }

    /** Only a trip that has not been picked up can be cancelled. */
    public Result<Trip> cancel(Long userId, String reason, Instant now) {
        if (status != TripStatus.SCHEDULED) return Result.failure(CommandError.conflict("trip-status-invalid"));
        if (reason == null || reason.isBlank() || reason.trim().length() > 255)
            return Result.failure(CommandError.validation("cancellation-reason-required"));
        cancelledAt = now;
        cancelledByUserId = userId;
        cancellationReason = reason.trim();
        return Result.success(changeStatus(TripStatus.CANCELLED, userId, now));
    }

    private Trip changeStatus(TripStatus newStatus, Long userId, Instant now) {
        history.add(new TripStatusChange(status, newStatus, userId, now));
        status = newStatus;
        return this;
    }
}
