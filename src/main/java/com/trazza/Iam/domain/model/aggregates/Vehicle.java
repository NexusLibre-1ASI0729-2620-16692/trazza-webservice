package com.trazza.platform.iam.domain.model.aggregates;

import com.trazza.platform.shared.domain.model.entities.AuditableEntity;
import com.trazza.platform.shared.domain.model.valueobjects.PublicIds;
import com.trazza.platform.shared.infrastructure.persistence.jpa.converters.PublicIdConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/** Truck of a fleet ({@code vehicles}): plate, model, maximum load and cargo box dimensions. */
@Entity
@Table(name = "vehicles")
@Getter
public class Vehicle extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PublicIdConverter.class)
    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "binary(16)")
    private UUID publicId;

    @Column(nullable = false)
    private Long carrierProfileId;

    @Column(nullable = false, length = 10)
    private String plate;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maxLoadKg;

    @Column(name = "cargo_length_m", nullable = false, precision = 5, scale = 2)
    private BigDecimal cargoLengthM;

    @Column(name = "cargo_width_m", nullable = false, precision = 5, scale = 2)
    private BigDecimal cargoWidthM;

    @Column(name = "cargo_height_m", nullable = false, precision = 5, scale = 2)
    private BigDecimal cargoHeightM;

    @Column(name = "cargo_volume_m3", nullable = false, precision = 7, scale = 2)
    private BigDecimal cargoVolumeM3;

    @Column(name = "photo_s3_key")
    private String photoS3Key;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected Vehicle() {
    }

    public Vehicle(Long carrierProfileId, String plate, String model, BigDecimal maxLoadKg, BigDecimal cargoLengthM,
                   BigDecimal cargoWidthM, BigDecimal cargoHeightM, BigDecimal cargoVolumeM3, String photoS3Key) {
        this.publicId = PublicIds.newId();
        this.carrierProfileId = carrierProfileId;
        update(plate, model, maxLoadKg, cargoLengthM, cargoWidthM, cargoHeightM, cargoVolumeM3, photoS3Key, true);
    }

    public void update(String plate, String model, BigDecimal maxLoadKg, BigDecimal cargoLengthM, BigDecimal cargoWidthM,
                       BigDecimal cargoHeightM, BigDecimal cargoVolumeM3, String photoS3Key, boolean active) {
        this.plate = plate;
        this.model = model;
        this.maxLoadKg = maxLoadKg;
        this.cargoLengthM = cargoLengthM;
        this.cargoWidthM = cargoWidthM;
        this.cargoHeightM = cargoHeightM;
        this.cargoVolumeM3 = cargoVolumeM3;
        this.photoS3Key = photoS3Key;
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }
}
