package com.trazza.trazzawebservice.iam.domain.model.aggregates;

import com.trazza.trazzawebservice.iam.domain.model.valueobjects.KycStatus;
import com.trazza.trazzawebservice.shared.domain.model.entities.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Personal data of the account owner ({@code person_identities}). DNI and phone are encrypted by the application
 * (AES-256-GCM); the DNI is also kept as an HMAC-SHA256 hash to check uniqueness.
 */
@Entity
@Table(name = "person_identities")
@Getter
public class PersonIdentity extends AuditableEntity {

    @Id
    private Long userId;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false)
    private byte[] dniEncrypted;

    @Column(nullable = false)
    private byte[] dniHash;

    @Column(name = "dni_last3", nullable = false, length = 3)
    private String dniLast3;

    @Column(nullable = false)
    private byte[] phoneEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus;

    protected PersonIdentity() {
    }

    public PersonIdentity(Long userId, String fullName, byte[] dniEncrypted, byte[] dniHash, String dniLast3,
                          byte[] phoneEncrypted) {
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
}
