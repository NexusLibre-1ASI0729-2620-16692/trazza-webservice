package com.trazza.iam.domain.model.aggregates;

import com.trazza.shared.domain.model.entities.IAuditableEntity;
import java.time.LocalDateTime;

/**
 * Merchant profile (merchant_profiles). The merchant needs a RUC (here) and a DNI (person identity).
 */
public class MerchantProfile implements IAuditableEntity {
    private Long id;
    private Long userId;
    private String businessName;
    private String ruc;
    private String businessType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected MerchantProfile() {
        this.businessName = "";
        this.ruc = "";
        this.businessType = "";
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

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getBusinessName() { return businessName; }
    public String getRuc() { return ruc; }
    public String getBusinessType() { return businessType; }
    @Override public LocalDateTime getCreatedAt() { return createdAt; }
    @Override public LocalDateTime getUpdatedAt() { return updatedAt; }
}