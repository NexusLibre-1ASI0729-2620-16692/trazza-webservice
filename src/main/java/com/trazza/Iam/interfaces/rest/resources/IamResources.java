package com.trazza.iam.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class IamResources {

    private IamResources() {
        // Clase contenedora, no instanciable
    }

    public record MerchantResource(String businessName, String ruc, String businessType) {}

    public record SignUpResource(
            String email,
            String password,
            String fullName,
            String dni,
            String phone,
            List<String> roles,
            MerchantResource merchant
    ) {}

    public record SignInResource(String email, String password) {}

    public record RefreshSessionResource(String refreshToken) {}

    public record IdentityResource(
            String fullName,
            String dniMasked,
            String phone,
            String kycStatus
    ) {}

    public record CarrierProfileResource(Long id) {}

    public record MerchantProfileResource(
            Long id,
            String businessName,
            String ruc,
            String businessType
    ) {}

    public record AccountResource(
            UUID id,
            String email,
            List<String> roles,
            IdentityResource identity,
            CarrierProfileResource carrierProfile,
            MerchantProfileResource merchantProfile,
            LocalDateTime createdAt
    ) {}

    public record AuthenticatedSessionResource(
            String accessToken,
            LocalDateTime accessTokenExpiresAt,
            String refreshToken,
            LocalDateTime refreshTokenExpiresAt,
            AccountResource user
    ) {}

    public record UpdateIdentityResource(String fullName, String phone) {}

    public record AddRoleResource(String role, MerchantResource merchant) {}

    public record UpdateMerchantProfileResource(String businessName, String businessType) {}

    public record SubmitIdentityVerificationResource(
            String documentFrontS3Key,
            String documentBackS3Key
    ) {}

    public record IdentityVerificationResource(
            Long id,
            String provider,
            String status,
            String externalReference,
            String rejectionReason,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt
    ) {}

    public record SaveVehicleResource(
            String plate,
            String model,
            BigDecimal maxLoadKg,
            BigDecimal cargoLengthM,
            BigDecimal cargoWidthM,
            BigDecimal cargoHeightM,
            BigDecimal cargoVolumeM3, // BigDecimal ya admite null
            String photoS3Key,
            Boolean isActive // Boolean admite null
    ) {}

    public record VehicleResource(
            UUID id,
            String plate,
            String model,
            BigDecimal maxLoadKg,
            BigDecimal cargoLengthM,
            BigDecimal cargoWidthM,
            BigDecimal cargoHeightM,
            BigDecimal cargoVolumeM3,
            String photoS3Key,
            Boolean isActive
    ) {}

    public record SaveDriverResource(
            String fullName,
            String sex,
            Integer experienceStars,
            Boolean isActive
    ) {}

    public record DriverResource(
            Long id,
            String fullName,
            String sex,
            Integer experienceStars,
            Boolean isActive
    ) {}

    public record CreateSavedAddressResource(
            String label,
            String address,
            String district,
            Double latitude,
            Double longitude
    ) {}

    public record SavedAddressResource(
            Long id,
            String label,
            String address,
            String district,
            Double latitude,
            Double longitude
    ) {}
}