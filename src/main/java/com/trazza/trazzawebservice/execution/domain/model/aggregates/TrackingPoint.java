package com.trazza.trazzawebservice.execution.domain.model.aggregates;

import com.trazza.trazzawebservice.execution.domain.model.valueobjects.TrackingSource;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;
import com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa.converters.GeoLocationConverter;
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

/** GPS point of an active trip ({@code tracking_points}, POINT column). */
@Entity
@Table(name = "tracking_points")
@Getter
public class TrackingPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @Convert(converter = GeoLocationConverter.class)
    @ColumnTransformer(read = "ST_AsText(location, 'axis-order=lat-long')", write = "ST_PointFromText(?, 4326, 'axis-order=lat-long')")
    @Column(name = "location", nullable = false, columnDefinition = "point")
    private GeoLocation location;

    @Column(precision = 5, scale = 1)
    private BigDecimal speedKmh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackingSource source;

    @Column(nullable = false)
    private Instant recordedAt;

    protected TrackingPoint() {
    }

    public TrackingPoint(Long tripId, GeoLocation location, BigDecimal speedKmh, TrackingSource source,
                         Instant recordedAt) {
        this.tripId = tripId;
        this.location = location;
        this.speedKmh = speedKmh;
        this.source = source;
        this.recordedAt = recordedAt;
    }
}
