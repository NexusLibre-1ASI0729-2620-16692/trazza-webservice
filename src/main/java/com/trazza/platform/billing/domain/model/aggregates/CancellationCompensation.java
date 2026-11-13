package com.trazza.platform.billing.domain.model.aggregates;

import com.trazza.platform.billing.domain.model.valueobjects.CompensationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cancellation_compensations")
@Getter
public class CancellationCompensation {

    @Id
    private Long tripId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompensationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant settledAt;

    protected CancellationCompensation() {
    }

    public CancellationCompensation(Long tripId, BigDecimal amount, String currency, Instant now) {
        this.tripId = tripId;
        this.amount = amount;
        this.currency = currency;
        this.status = CompensationStatus.PENDING;
        this.createdAt = now;
    }

    public boolean isOpen() {
        return status == CompensationStatus.PENDING || status == CompensationStatus.DISPUTED;
    }

    public void confirmPaid(Instant now) {
        settle(CompensationStatus.PAID_EXTERNALLY, now);
    }

    public void waive(Instant now) {
        settle(CompensationStatus.WAIVED, now);
    }

    public void dispute() {
        if (status == CompensationStatus.PENDING) status = CompensationStatus.DISPUTED;
    }

    private void settle(CompensationStatus newStatus, Instant now) {
        if (!isOpen()) return;
        status = newStatus;
        settledAt = now;
    }
}
