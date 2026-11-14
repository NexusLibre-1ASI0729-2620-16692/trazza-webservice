package com.trazza.iam.infrastructure.persistence.entityframeworkcore.configuration.extensions;

import com.trazza.iam.domain.model.aggregates.*;
import com.trazza.iam.domain.model.valueobjects.IamEnums.*;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

/**
 * Mapping of the IAM & Profiles tables of trazza_database.
 * saved_addresses is handled with SQL.
 */
public class ModelBuilderExtensions {

    // -------------------------------------------------------------------------
    // MAPEO DE ENTIDADES (Equivalente a builder.Entity<T>())
    // -------------------------------------------------------------------------

    @Entity
    @Table(name = "users")
    public static class UserEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY) // ValueGeneratedOnAdd
        private Long id;

        @Column(name = "public_id", nullable = false)
        private UUID publicId;

        @Column(nullable = false, length = 255)
        private String email;

        @Column(nullable = false, length = 255)
        private String passwordHash;

        @Transient // Ignore
        private Boolean canSignIn;

        @OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private List<UserRoleEntity> roles;
    }

    @Entity
    @Table(name = "user_roles")
    @IdClass(UserRoleId.class)
    public static class UserRoleEntity {
        @Id
        @Column(name = "user_id")
        private Long userId;

        @Id
        @Enumerated(EnumType.STRING) // HasEnumText
        @Column(name = "role")
        private UserRoleType role;
    }

    public static class UserRoleId implements java.io.Serializable {
        private Long userId;
        private UserRoleType role;
    }

    @Entity
    @Table(name = "person_identities")
    public static class PersonIdentityEntity {
        @Id
        @Column(name = "user_id")
        private Long userId; // ValueGeneratedNever

        @Column(name = "full_name", nullable = false, length = 200)
        private String fullName;

        @Enumerated(EnumType.STRING) // HasEnumText
        @Column(name = "kyc_status")
        private KycStatus kycStatus;
    }

    @Entity
    @Table(name = "identity_verifications")
    public static class IdentityVerificationEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY) // ValueGeneratedOnAdd
        private Long id;

        @Enumerated(EnumType.STRING) // HasEnumText
        @Column(name = "status")
        private VerificationStatus status;
    }

    @Entity
    @Table(name = "refresh_tokens")
    public static class RefreshTokenEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY) // ValueGeneratedOnAdd
        private Long id;
    }

    @Entity
    @Table(name = "carrier_profiles")
    public static class CarrierProfileEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY) // ValueGeneratedOnAdd
        private Long id;
    }

    @Entity
    @Table(name = "merchant_profiles")
    public static class MerchantProfileEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY) // ValueGeneratedOnAdd
        private Long id;
    }

    @Entity
    @Table(name = "drivers")
    public static class DriverEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY) // ValueGeneratedOnAdd
        private Long id;

        @Enumerated(EnumType.STRING) // HasNullableEnumText
        @Column(name = "sex")
        private DriverSex sex;
    }

    @Entity
    @Table(name = "vehicles")
    public static class VehicleEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY) // ValueGeneratedOnAdd
        private Long id;

        @Column(name = "public_id", nullable = false)
        private UUID publicId;
    }
}