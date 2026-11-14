package com.trazza.trazzawebservice.iam.domain.model.aggregates;

import com.trazza.trazzawebservice.shared.domain.model.entities.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** Merchant profile ({@code merchant_profiles}). The merchant needs a RUC (here) and a DNI (person identity). */
@Entity
@Table(name = "merchant_profiles")
@Getter
public class MerchantProfile extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(nullable = false)
    private String businessType;

    protected MerchantProfile() {
    }

    public MerchantProfile(Long userId, String businessName, String ruc, String businessType) {
        this.userId = userId;
        this.businessName = businessName;
        this.ruc = ruc;
        this.businessType = businessType;
    }

    public void update(String businessName, String businessType) {
        this.businessName = businessName;
        this.businessType = businessType;
    }
}
