package com.trazza.iam.domain.model.aggregates;

import com.trazza.iam.domain.model.valueobjects.KycStatus;
import com.trazza.shared.domain.model.entities.IAuditableEntity;
import java.time.LocalDateTime;

/**
 * Personal data of the account owner (person_identities). DNI and phone are encrypted by the
 * application (AES-256-GCM); the DNI is also kept as an HMAC-SHA256 hash to check uniqueness.
 */
public class PersonIdentity implements IAuditableEntity {
    private Long userId;
    private String fullName;
    private byte[] dniEncrypted;
    private byte[] dniHash;
    private String dniLast3;
    private byte[] phoneEncrypted;
    private KycStatus kycStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected PersonIdentity() {
        this.fullName = "";
        this.dniEncrypted = new byte[0];
        this.dniHash = new byte[0];
        this.dniLast3 = "";
        this.phoneEncrypted = new byte[0];
    }

    public PersonIdentity(Long userId, String fullName, byte[] dniEncrypted, byte[] dniHash,
                          String dniLast3, byte[] phoneEncrypted) {
        this.userId = userId;
        this.fullName = fullName;
        this.dniEncrypted = dniEncrypted;
        this.dniHash = dniHash;
        this.dniLast3 = dniLast3;
        this.phoneEncrypted = phoneEncrypted;
        this.kycStatus = KycStatus.PENDING;
    }

    public void updateContact(String fullName, byte[] phoneEncrypted) {
        this.fullName = fullName;
        this.phoneEncrypted = phoneEncrypted;
    }

    public void setKycStatus(KycStatus status) {
        this.kycStatus = status;
    }

    // Getters
    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public byte[] getDniEncrypted() { return dniEncrypted; }
    public byte[] getDniHash() { return dniHash; }
    public String getDniLast3() { return dniLast3; }
    public byte[] getPhoneEncrypted() { return phoneEncrypted; }
    public KycStatus getKycStatus() { return kycStatus; }
    @Override public LocalDateTime getCreatedAt() { return createdAt; }
    @Override public LocalDateTime getUpdatedAt() { return updatedAt; }
}