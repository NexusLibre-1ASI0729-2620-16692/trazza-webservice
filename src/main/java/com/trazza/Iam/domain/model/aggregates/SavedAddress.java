package com.trazza.platform.iam.domain.model.aggregates;

import com.trazza.platform.shared.domain.model.valueobjects.GeoLocation;
import com.trazza.platform.shared.infrastructure.persistence.jpa.converters.GeoLocationConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

/** Frequent address of a user ({@code saved_addresses}), e.g. "Almacén" or "Tienda Gamarra", with real coordinates. */
@Entity
@Table(name = "saved_addresses")
@Getter
public class SavedAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(nullable = false)
    private String address;

    @Column(length = 80)
    private String district;

    @Convert(converter = GeoLocationConverter.class)
    @ColumnTransformer(read = "ST_AsText(location, 'axis-order=lat-long')", write = "ST_PointFromText(?, 4326, 'axis-order=lat-long')")
    @Column(name = "location", nullable = false, columnDefinition = "point")
    private GeoLocation location;

    @Column(nullable = false)
    private Instant createdAt;

    protected SavedAddress() {
    }

    public SavedAddress(Long userId, String label, String address, String district, GeoLocation location, Instant now) {
        this.userId = userId;
        this.label = label;
        this.address = address;
        this.district = district;
        this.location = location;
        this.createdAt = now;
    }
}
