package com.trazza.trazzawebservice.iam.domain.model.aggregates;

import com.trazza.trazzawebservice.shared.domain.model.entities.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** Carrier profile ({@code carrier_profiles}): owner of the fleet (vehicles and drivers). */
@Entity
@Table(name = "carrier_profiles")
@Getter
public class CarrierProfile extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    protected CarrierProfile() {
    }

    public CarrierProfile(Long userId) {
        this.userId = userId;
    }
}
