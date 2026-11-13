package com.trazza.trazzawebservice.billing.domain.model.aggregates;

import com.trazza.trazzawebservice.billing.domain.model.entities.SubscriptionVehicle;
import com.trazza.trazzawebservice.billing.domain.model.valueobjects.SubscriptionStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Entity
@Table(name = "subscriptions")
@Getter
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private Instant currentPeriodStart;

    @Column(nullable = false)
    private Instant currentPeriodEnd;

    @Column(length = 100)
    private String gatewaySubscriptionId;

    private Instant cancelledAt;

    @Column(nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subscription_vehicles", joinColumns = @JoinColumn(name = "subscription_id"))
    private List<SubscriptionVehicle> vehicles = new ArrayList<>();

    protected Subscription() {
    }

    public Subscription(Long userId, Integer planId, Collection<Long> vehicleIds, String gatewaySubscriptionId,
                        Instant now) {
        this.userId = userId;
        this.planId = planId;
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodStart = now;
        this.currentPeriodEnd = plusOneMonth(now);
        this.gatewaySubscriptionId = gatewaySubscriptionId;
        this.createdAt = now;
        for (var vehicleId : new LinkedHashSet<>(vehicleIds)) vehicles.add(new SubscriptionVehicle(vehicleId, now));
    }

    public List<SubscriptionVehicle> getVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    public List<Long> vehicleIds() {
        return vehicles.stream().map(SubscriptionVehicle::getVehicleId).toList();
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public void renew() {
        currentPeriodStart = currentPeriodEnd;
        currentPeriodEnd = plusOneMonth(currentPeriodEnd);
        status = SubscriptionStatus.ACTIVE;
    }

    public void cancel(Instant now) {
        if (status == SubscriptionStatus.CANCELLED) return;
        status = SubscriptionStatus.CANCELLED;
        cancelledAt = now;
    }

    private static Instant plusOneMonth(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
    }
}