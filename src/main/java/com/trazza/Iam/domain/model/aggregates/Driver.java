package com.trazza.platform.iam.domain.model.aggregates;

import com.trazza.platform.iam.domain.model.valueobjects.DriverSex;
import com.trazza.platform.shared.domain.model.entities.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** Driver of a fleet ({@code drivers}). Drivers have no account: only name, optional sex and experience (1 to 5 stars). */
@Entity
@Table(name = "drivers")
@Getter
public class Driver extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long carrierProfileId;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Enumerated(EnumType.STRING)
    private DriverSex sex;

    @Column(nullable = false)
    private int experienceStars;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected Driver() {
    }

    public Driver(Long carrierProfileId, String fullName, DriverSex sex, int experienceStars) {
        this.carrierProfileId = carrierProfileId;
        this.fullName = fullName;
        this.sex = sex;
        this.experienceStars = experienceStars;
        this.active = true;
    }

    public void update(String fullName, DriverSex sex, int experienceStars, boolean active) {
        this.fullName = fullName;
        this.sex = sex;
        this.experienceStars = experienceStars;
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }
}
