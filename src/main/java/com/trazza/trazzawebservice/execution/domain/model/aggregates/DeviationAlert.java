package com.trazza.trazzawebservice.execution.domain.model.aggregates;

import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;
import com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa.converters.GeoLocationConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.time.Instant;

/** The truck left the expected path by more than the allowed distance ({@code deviation_alerts}). */
@Entity
@Table(name = "deviation_alerts")
@Getter
public class DeviationAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @Convert(converter = GeoLocationConverter.class)
    @ColumnTransformer(read = "ST_AsText(location, 'axis-order=lat-long')", write = "ST_PointFromText(?, 4326, 'axis-order=lat-long')")
    @Column(name = "location", nullable = false, columnDefinition = "point")
    private GeoLocation location;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal deviationKm;

    @Column(nullable = false)
    private Instant detectedAt;

    private Instant notifiedAt;
    private Instant acknowledgedAt;

    protected DeviationAlert() {
    }

    public DeviationAlert(Long tripId, GeoLocation location, BigDecimal deviationKm, Instant now) {
        this.tripId = tripId;
        this.location = location;
        this.deviationKm = deviationKm;
        this.detectedAt = now;
        this.notifiedAt = now;
    }

    public void acknowledge(Instant now) {
        if (acknowledgedAt == null) acknowledgedAt = now;
    }
}
