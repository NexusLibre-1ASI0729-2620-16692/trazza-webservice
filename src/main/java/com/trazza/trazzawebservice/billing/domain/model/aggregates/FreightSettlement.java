package com.trazza.trazzawebservice.billing.domain.model.aggregates;

import com.trazza.trazzawebservice.billing.domain.model.valueobjects.SettlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "freight_settlements")
@Getter
public class FreightSettlement {

    @Id
    private Long tripId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    private Instant merchantMarkedPaidAt;
    private Instant carrierConfirmedAt;

    @Column(length = 100)
    private String externalReference;

    protected FreightSettlement() {
    }

    public FreightSettlement(Long tripId, BigDecimal amount, String currency) {
        this.tripId = tripId;
        this.amount = amount;
        this.currency = currency;
    }

    public SettlementStatus status() {
        if (carrierConfirmedAt != null) return SettlementStatus.SETTLED;
        return merchantMarkedPaidAt != null ? SettlementStatus.MARKED_PAID : SettlementStatus.PENDING;
    }

    public void markPaid(String reference, Instant now) {
        if (merchantMarkedPaidAt == null) merchantMarkedPaidAt = now;
        if (reference != null && !reference.isBlank()) externalReference = reference.trim();
    }

    public void confirmReceived(Instant now) {
        if (carrierConfirmedAt == null) carrierConfirmedAt = now;
    }
}
