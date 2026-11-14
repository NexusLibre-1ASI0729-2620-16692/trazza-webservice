package com.trazza.trazzawebservice.matchmaking.domain.model.aggregates;

import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.ReturnRouteStatus;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Empty return trip published by a carrier. It can take at most two shipments; the free capacity is computed from
 * the vehicle and the accepted proposals, never stored.
 */
@Entity
@Table(name = "return_routes")
@Getter
public class ReturnRoute extends AuditableEntity {

    public static final int MAX_SHIPMENTS_LIMIT = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PublicIdConverter.class)
    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "binary(16)")
    private UUID publicId;

    @Column(nullable = false)
    private Long carrierProfileId;

    @Column(nullable = false)
    private Long vehicleId;

    private Long driverId;

    @Column(nullable = false)
    private String originAddress;

    @Convert(converter = GeoLocationConverter.class)
    @ColumnTransformer(read = "ST_AsText(origin_location, 'axis-order=lat-long')", write = "ST_PointFromText(?, 4326, 'axis-order=lat-long')")
    @Column(name = "origin_location", nullable = false, columnDefinition = "point")
    private GeoLocation origin;

    @Column(nullable = false)
    private String destinationAddress;

    @Convert(converter = GeoLocationConverter.class)
    @ColumnTransformer(read = "ST_AsText(destination_location, 'axis-order=lat-long')",
            write = "ST_PointFromText(?, 4326, 'axis-order=lat-long')")
    @Column(name = "destination_location", nullable = false, columnDefinition = "point")
    private GeoLocation destination;

    @Column(nullable = false)
    private Instant departureAt;

    @Column(nullable = false)
    private int maxShipments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnRouteStatus status;

    protected ReturnRoute() {
    }

    public ReturnRoute(Long carrierProfileId, Long vehicleId, Long driverId, String originAddress, GeoLocation origin,
                       String destinationAddress, GeoLocation destination, Instant departureAt, int maxShipments) {
        this.publicId = PublicIds.newId();
        this.carrierProfileId = carrierProfileId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.originAddress = originAddress.trim();
        this.origin = origin;
        this.destinationAddress = destinationAddress.trim();
        this.destination = destination;
        this.departureAt = departureAt;
        this.maxShipments = maxShipments;
        this.status = ReturnRouteStatus.PUBLISHED;
    }

    public boolean isOpenForProposals() {
        return status == ReturnRouteStatus.PUBLISHED;
    }

    public boolean isClosed() {
        return status == ReturnRouteStatus.COMPLETED || status == ReturnRouteStatus.CANCELLED;
    }

    public void markFull() {
        if (status == ReturnRouteStatus.PUBLISHED) status = ReturnRouteStatus.FULL;
    }

    public void reopen() {
        if (status == ReturnRouteStatus.FULL) status = ReturnRouteStatus.PUBLISHED;
    }

    public void start() {
        if (status == ReturnRouteStatus.PUBLISHED || status == ReturnRouteStatus.FULL)
            status = ReturnRouteStatus.IN_PROGRESS;
    }

    public void complete() {
        if (!isClosed()) status = ReturnRouteStatus.COMPLETED;
    }

    public void cancel() {
        if (!isClosed()) status = ReturnRouteStatus.CANCELLED;
    }
}
