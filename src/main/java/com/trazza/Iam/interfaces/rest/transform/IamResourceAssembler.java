package com.trazza.iam.interfaces.rest.transform;

import com.trazza.iam.domain.model.aggregates.*;
import com.trazza.iam.domain.model.commands.FleetCommands.*;
import com.trazza.iam.domain.model.commands.IamCommands.*;
import com.trazza.iam.domain.model.valueobjects.IamEnums.*;
import com.trazza.iam.domain.services.IamServices.*;
import com.trazza.iam.interfaces.rest.resources.IamResources.*;
import com.trazza.shared.domain.model.CommandError;
import com.trazza.shared.domain.model.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Converts IAM resources into commands and aggregates into resources.
 */
public final class IamResourceAssembler {

    private IamResourceAssembler() {
        // Clase de utilidad, no instanciable
    }

    public static Result<SignUpCommand, CommandError> toCommand(SignUpResource resource) {
        List<UserRoleType> roles = new ArrayList<>();
        if (resource.roles() != null) {
            for (String text : resource.roles()) {
                try {
                    roles.add(UserRoleType.valueOf(text.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return Result.failure(CommandError.validation("role-invalid"));
                }
            }
        }

        return Result.success(new SignUpCommand(
                resource.email(),
                resource.password(),
                resource.fullName(),
                resource.dni(),
                resource.phone(),
                roles,
                toMerchantData(resource.merchant())
        ));
    }

    public static Result<AddRoleCommand, CommandError> toCommand(Long userId, AddRoleResource resource) {
        try {
            UserRoleType role = UserRoleType.valueOf(resource.role().toUpperCase());
            return Result.success(new AddRoleCommand(userId, role, toMerchantData(resource.merchant())));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Result.failure(CommandError.validation("role-invalid"));
        }
    }

    public static Result<DriverData, CommandError> toDriverData(SaveDriverResource resource) {
        DriverSex sex = null;
        if (resource.sex() != null && !resource.sex().isBlank()) {
            try {
                sex = DriverSex.valueOf(resource.sex().toUpperCase());
            } catch (IllegalArgumentException e) {
                return Result.failure(CommandError.validation("sex-invalid"));
            }
        }

        Boolean isActive = resource.isActive() != null ? resource.isActive() : true;
        return Result.success(new DriverData(resource.fullName(), sex, resource.experienceStars(), isActive));
    }

    public static VehicleData toVehicleData(SaveVehicleResource resource) {
        Boolean isActive = resource.isActive() != null ? resource.isActive() : true;
        return new VehicleData(
                resource.plate(), resource.model(), resource.maxLoadKg(),
                resource.cargoLengthM(), resource.cargoWidthM(), resource.cargoHeightM(),
                resource.cargoVolumeM3(), resource.photoS3Key(), isActive
        );
    }

    public static AccountResource toResource(UserAccount account) {
        // Roles ordenados alfabéticamente
        List<String> roles = account.user().getRoles().stream()
                .map(r -> r.getRole().name())
                .sorted()
                .toList();

        IdentityResource identityResource = account.identity() == null ? null :
                new IdentityResource(
                        account.identity().getFullName(),
                        "*****" + account.identity().getDniLast3(),
                        account.phone(),
                        account.identity().getKycStatus().name()
                );

        CarrierProfileResource carrierResource = account.carrierProfile() == null ? null :
                new CarrierProfileResource(account.carrierProfile().getId());

        MerchantProfileResource merchantResource = account.merchantProfile() == null ? null :
                new MerchantProfileResource(
                        account.merchantProfile().getId(),
                        account.merchantProfile().getBusinessName(),
                        account.merchantProfile().getRuc(),
                        account.merchantProfile().getBusinessType()
                );

        return new AccountResource(
                account.user().getPublicId(),
                account.user().getEmail(),
                roles,
                identityResource,
                carrierResource,
                merchantResource,
                account.user().getCreatedAt()
        );
    }

    public static AuthenticatedSessionResource toResource(AuthenticatedSession session) {
        return new AuthenticatedSessionResource(
                session.accessToken(),
                session.accessTokenExpiresAt(),
                session.refreshToken(),
                session.refreshTokenExpiresAt(),
                toResource(session.account())
        );
    }

    public static IdentityVerificationResource toResource(IdentityVerification verification) {
        return new IdentityVerificationResource(
                verification.getId(),
                verification.getProvider(),
                verification.getStatus().name(),
                verification.getExternalReference(),
                verification.getRejectionReason(),
                verification.getSubmittedAt(),
                verification.getReviewedAt()
        );
    }

    public static VehicleResource toResource(Vehicle vehicle) {
        return new VehicleResource(
                vehicle.getPublicId(),
                vehicle.getPlate(),
                vehicle.getModel(),
                vehicle.getMaxLoadKg(),
                vehicle.getCargoLengthM(),
                vehicle.getCargoWidthM(),
                vehicle.getCargoHeightM(),
                vehicle.getCargoVolumeM3(),
                vehicle.getPhotoS3Key(),
                vehicle.getIsActive()
        );
    }

    public static DriverResource toResource(Driver driver) {
        return new DriverResource(
                driver.getId(),
                driver.getFullName(),
                driver.getSex() != null ? driver.getSex().name() : null,
                driver.getExperienceStars().intValue(), // De Byte a Integer
                driver.getIsActive()
        );
    }

    public static SavedAddressResource toResource(SavedAddress address) {
        return new SavedAddressResource(
                address.id(),
                address.label(),
                address.address(),
                address.district(),
                address.latitude(),
                address.longitude()
        );
    }

    private static MerchantData toMerchantData(MerchantResource resource) {
        return resource == null ? null : new MerchantData(resource.businessName(), resource.ruc(), resource.businessType());
    }
}