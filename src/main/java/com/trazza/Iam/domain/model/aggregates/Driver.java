package com.trazza.iam.domain.model.aggregates;

import com.trazza.iam.domain.model.valueobjects.DriverSex;
import com.trazza.shared.domain.model.entities.IAuditableEntity;
import java.time.LocalDateTime;

public class Driver implements IAuditableEntity {
    private Long id;
    private Long carrierProfileId;
    private String fullName;
    private DriverSex sex;
    private byte experienceStars;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Driver() { this.fullName = ""; }

    public Driver(Long carrierProfileId, String fullName, DriverSex sex, byte experienceStars) {
        this.carrierProfileId = carrierProfileId;
        this.fullName = fullName;
        this.sex = sex;
        this.experienceStars = experienceStars;
        this.isActive = true;
    }

    public Long getId() { return id; }
    public Long getCarrierProfileId() { return carrierProfileId; }
    public String getFullName() { return fullName; }
    public DriverSex getSex() { return sex; }
    public byte getExperienceStars() { return experienceStars; }
    public boolean isActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void update(String fullName, DriverSex sex, byte experienceStars, boolean isActive) {
        this.fullName = fullName;
        this.sex = sex;
        this.experienceStars = experienceStars;
        this.isActive = isActive;
    }

    public void deactivate() { this.isActive = false; }
}