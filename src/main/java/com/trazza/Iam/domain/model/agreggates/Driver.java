package com.trazza.iam.domain.model.aggregates;

import com.trazza.iam.domain.model.valueobjects.DriverSex;
import com.trazza.shared.domain.model.entities.IAuditableEntity;
import java.time.LocalDateTime;

/**
 * Driver of a fleet (drivers). Drivers have no account: only name, optional sex and experience (1 to 5 stars).
 */
public class Driver implements IAuditableEntity {
    private Long id;
    private Long carrierProfileId;
    private String fullName;
    private DriverSex sex;
    private Byte experienceStars;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Driver() {
        this.fullName = "";
    }

    public Driver(Long carrierProfileId, String fullName, DriverSex sex, Byte experienceStars) {
        this.carrierProfileId = carrierProfileId;
        this.fullName = fullName;
        this.sex = sex;
        this.experienceStars = experienceStars;
        this.isActive = true;
    }

    public void update(String fullName, DriverSex sex, Byte experienceStars, Boolean isActive) {
        this.fullName = fullName;
        this.sex = sex;
        this.experienceStars = experienceStars;
        this.isActive = isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }

    // Getters
    public Long getId() { return id; }
    public Long getCarrierProfileId() { return carrierProfileId; }
    public String getFullName() { return fullName; }
    public DriverSex getSex() { return sex; }
    public Byte getExperienceStars() { return experienceStars; }
    public Boolean getIsActive() { return isActive; }
    @Override public LocalDateTime getCreatedAt() { return createdAt; }
    @Override public LocalDateTime getUpdatedAt() { return updatedAt; }
}