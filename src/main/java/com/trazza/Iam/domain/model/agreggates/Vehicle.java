package com.trazza.iam.domain.model.aggregates;

import com.trazza.shared.domain.model.entities.IAuditableEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Truck of a fleet (vehicles): plate, model, maximum load and cargo box dimensions.
 */
public class Vehicle implements IAuditableEntity {
    private Long id;
    private UUID publicId;
    private Long carrierProfileId;
    private String plate;
    private String model;
    private BigDecimal maxLoadKg;
    private BigDecimal cargoLengthM;
    private BigDecimal cargoWidthM;
    private BigDecimal cargoHeightM;
    private BigDecimal cargoVolumeM3;
    private String photoS3Key;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Vehicle() {
        this.plate = "";
        this.model = "";
    }

    public Vehicle(Long carrierProfileId, String plate, String model, BigDecimal maxLoadKg,
                   BigDecimal cargoLengthM, BigDecimal cargoWidthM, BigDecimal cargoHeightM,
                   BigDecimal cargoVolumeM3, String photoS3Key) {
        this.publicId = UUID.randomUUID(); // Equivalente a Guid.CreateVersion7() (en Java 21+ usa UUID v7 si está disponible)
        this.carrierProfileId = carrierProfileId;
        this.isActive = true;
        this.plate = plate;
        this.model = model;
        update(plate, model, maxLoadKg, cargoLengthM, cargoWidthM, cargoHeightM, cargoVolumeM3, photoS3Key, true);
    }

    public void update(String plate, String model, BigDecimal maxLoadKg, BigDecimal cargoLengthM,
                       BigDecimal cargoWidthM, BigDecimal cargoHeightM, BigDecimal cargoVolumeM3,
                       String photoS3Key, Boolean isActive) {
        this.plate = plate;
        this.model = model;
        this.maxLoadKg = maxLoadKg;
        this.cargoLengthM = cargoLengthM;
        this.cargoWidthM = cargoWidthM;
        this.cargoHeightM = cargoHeightM;
        this.cargoVolumeM3 = cargoVolumeM3;
        this.photoS3Key = photoS3Key;
        this.isActive = isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }

    // Getters
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Long getCarrierProfileId() { return carrierProfileId; }
    public String getPlate() { return plate; }
    public String getModel() { return model; }
    public BigDecimal getMaxLoadKg() { return maxLoadKg; }
    public BigDecimal getCargoLengthM() { return cargoLengthM; }
    public BigDecimal getCargoWidthM() { return cargoWidthM; }
    public BigDecimal getCargoHeightM() { return cargoHeightM; }
    public BigDecimal getCargoVolumeM3() { return cargoVolumeM3; }
    public String getPhotoS3Key() { return photoS3Key; }
    public Boolean getIsActive() { return isActive; }
    @Override public LocalDateTime getCreatedAt() { return createdAt; }
    @Override public LocalDateTime getUpdatedAt() { return updatedAt; }
}