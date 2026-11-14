package com.trazza.trazzawebservice.matchmaking.domain.model.aggregates;

import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.MatchProposalStatus;
import com.trazza.trazzawebservice.shared.domain.model.entities.AuditableEntity;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.PublicIds;
import com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa.converters.PublicIdConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The single offer a carrier makes to carry a shipment on one of its return routes. There are no counter-offers:
 * the merchant accepts or rejects it.
 */
@Entity
@Table(name = "match_proposals")
@Getter
public class MatchProposal extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PublicIdConverter.class)
    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "binary(16)")
    private UUID publicId;

    @Column(nullable = false)
    private Long shipmentRequestId;

    @Column(nullable = false)
    private Long returnRouteId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal offeredRate;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchProposalStatus status;

    private Instant respondedAt;

    protected MatchProposal() {
    }

    public MatchProposal(Long shipmentRequestId, Long returnRouteId, BigDecimal offeredRate, String currency) {
        this.publicId = PublicIds.newId();
        this.shipmentRequestId = shipmentRequestId;
        this.returnRouteId = returnRouteId;
        this.offeredRate = offeredRate;
        this.currency = currency;
        this.status = MatchProposalStatus.PENDING;
    }

    public boolean isPending() {
        return status == MatchProposalStatus.PENDING;
    }

    public void accept(Instant now) {
        respond(MatchProposalStatus.ACCEPTED, now);
    }

    public void reject(Instant now) {
        respond(MatchProposalStatus.REJECTED, now);
    }

    public void cancel(Instant now) {
        respond(MatchProposalStatus.CANCELLED, now);
    }

    public void expire(Instant now) {
        respond(MatchProposalStatus.EXPIRED, now);
    }

    /** The trip of an accepted proposal was cancelled: the slot on the route is released. */
    public void withdraw(Instant now) {
        if (status != MatchProposalStatus.ACCEPTED) return;
        status = MatchProposalStatus.CANCELLED;
        respondedAt = now;
    }

    private void respond(MatchProposalStatus newStatus, Instant now) {
        if (!isPending()) return;
        status = newStatus;
        respondedAt = now;
    }
}
