package com.trazza.trazzawebservice.reputation.domain.model.aggregates;

import com.trazza.trazzawebservice.reputation.domain.model.valueobjects.LoyaltyReason;
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

@Entity
@Table(name = "loyalty_point_entries")
@Getter
public class LoyaltyPointEntry {

    public static final int TRIP_COMPLETED_POINTS = 10;
    public static final int RATING_GIVEN_POINTS = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long tripId;

    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyReason reason;

    @Column(nullable = false)
    private Instant createdAt;

    protected LoyaltyPointEntry() {
    }

    public LoyaltyPointEntry(Long userId, Long tripId, int points, LoyaltyReason reason, Instant now) {
        this.userId = userId;
        this.tripId = tripId;
        this.points = points;
        this.reason = reason;
        this.createdAt = now;
    }
}