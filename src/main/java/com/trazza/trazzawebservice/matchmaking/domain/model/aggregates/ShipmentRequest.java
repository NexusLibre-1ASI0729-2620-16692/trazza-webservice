package com.trazza.trazzawebservice.matchmaking.domain.model.aggregates;

import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.ShipmentRequestStatus;
import com.trazza.trazzawebservice.shared.domain.model.entities.AuditableEntity;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.PublicIds;
import com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa.converters.GeoLocationConverter;
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
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Cargo a merchant needs to move, with real pick-up and delivery coordinates and a pick-up time window. */
@Entity
@Table(name = "shipment_requests")
@Getter
public class ShipmentRequest extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PublicIdConverter.class)
    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "binary(16)")
    private UUID publicId;

    @Column(nullable = false)
    private Long merchantProfileId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", nullable = false, precision = 7, scale = 2)
    private BigDecimal volumeM3;

    @Column(nullable = false)
    private String pickupAddress;

    @Convert(converter = GeoLocationConverter.class)
    @ColumnTransformer(read = "ST_AsText(pickup_location, 'axis-order=lat-long')", write = "ST_PointFromText(?, 4326, 'axis-order=lat-long')")
    @Column(name = "pickup_location", nullable = false, columnDefinition = "point")
    private GeoLocation pickup;

    @Column(nullable = false)
    private String deliveryAddress;

    @Convert(converter = GeoLocationConverter.class)
    @ColumnTransformer(read = "ST_AsText(delivery_location, 'axis-order=lat-long')",
            write = "ST_PointFromText(?, 4326, 'axis-order=lat-long')")
    @Column(name = "delivery_location", nullable = false, columnDefinition = "point")
    private GeoLocation delivery;

    @Column(nullable = false)
    private Instant pickupFrom;

    @Column(nullable = false)
    private Instant pickupUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentRequestStatus status;

    private Instant cancelledAt;
    private String cancellationReason;

    protected ShipmentRequest() {
    }

    public ShipmentRequest(Long merchantProfileId, String description, BigDecimal weightKg, BigDecimal volumeM3,
                           String pickupAddress, GeoLocation pickup, String deliveryAddress, GeoLocation delivery,
                           Instant pickupFrom, Instant pickupUntil) {
        this.publicId = PublicIds.newId();
        this.merchantProfileId = merchantProfileId;
        this.description = description.trim();
        this.weightKg = weightKg;
        this.volumeM3 = volumeM3;
        this.pickupAddress = pickupAddress.trim();
        this.pickup = pickup;
        this.deliveryAddress = deliveryAddress.trim();
        this.delivery = delivery;
        this.pickupFrom = pickupFrom;
        this.pickupUntil = pickupUntil;
        this.status = ShipmentRequestStatus.OPEN;
    }

    public void markMatched() {
        if (status == ShipmentRequestStatus.OPEN) status = ShipmentRequestStatus.MATCHED;
    }

    public void markInTransit() {
        if (status == ShipmentRequestStatus.MATCHED) status = ShipmentRequestStatus.IN_TRANSIT;
    }

    public void markDelivered() {
        if (status == ShipmentRequestStatus.MATCHED || status == ShipmentRequestStatus.IN_TRANSIT)
            status = ShipmentRequestStatus.DELIVERED;
    }

    /** The carrier dropped the trip before picking up: the merchant can receive new offers. */
    public void reopen() {
        if (status == ShipmentRequestStatus.MATCHED) status = ShipmentRequestStatus.OPEN;
    }

    public void cancel(String reason, Instant now) {
        if (status == ShipmentRequestStatus.DELIVERED || status == ShipmentRequestStatus.CANCELLED) return;
        status = ShipmentRequestStatus.CANCELLED;
        cancellationReason = reason.trim();
        cancelledAt = now;
    }
}
