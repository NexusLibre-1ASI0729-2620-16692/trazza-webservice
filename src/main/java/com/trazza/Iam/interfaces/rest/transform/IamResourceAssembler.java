package com.trazza.platform.iam.interfaces.rest.transform;

import com.trazza.platform.iam.domain.model.aggregates.Driver;
import com.trazza.platform.iam.domain.model.aggregates.IdentityVerification;
import com.trazza.platform.iam.domain.model.aggregates.SavedAddress;
import com.trazza.platform.iam.domain.model.aggregates.Vehicle;
import com.trazza.platform.iam.domain.model.commands.AddRoleCommand;
import com.trazza.platform.iam.domain.model.commands.DriverData;
import com.trazza.platform.iam.domain.model.commands.MerchantData;
import com.trazza.platform.iam.domain.model.commands.SignUpCommand;
import com.trazza.platform.iam.domain.model.commands.VehicleData;
import com.trazza.platform.iam.domain.model.valueobjects.DriverSex;
import com.trazza.platform.iam.domain.model.valueobjects.UserRoleType;
import com.trazza.platform.iam.domain.services.AuthenticatedSession;
import com.trazza.platform.iam.domain.services.UserAccount;
import com.trazza.platform.iam.interfaces.rest.resources.AccountResource;
import com.trazza.platform.iam.interfaces.rest.resources.AddRoleResource;
import com.trazza.platform.iam.interfaces.rest.resources.AuthenticatedSessionResource;
import com.trazza.platform.iam.interfaces.rest.resources.CarrierProfileResource;
import com.trazza.platform.iam.interfaces.rest.resources.DriverResource;
import com.trazza.platform.iam.interfaces.rest.resources.IdentityResource;
import com.trazza.platform.iam.interfaces.rest.resources.IdentityVerificationResource;
import com.trazza.platform.iam.interfaces.rest.resources.MerchantProfileResource;
import com.trazza.platform.iam.interfaces.rest.resources.MerchantResource;
import com.trazza.platform.iam.interfaces.rest.resources.SaveDriverResource;
import com.trazza.platform.iam.interfaces.rest.resources.SaveVehicleResource;
import com.trazza.platform.iam.interfaces.rest.resources.SavedAddressResource;
import com.trazza.platform.iam.interfaces.rest.resources.SignUpResource;
import com.trazza.platform.iam.interfaces.rest.resources.VehicleResource;
import com.trazza.platform.shared.domain.model.CommandError;
import com.trazza.platform.shared.domain.model.Result;
import com.trazza.platform.shared.domain.model.valueobjects.EnumText;

import java.util.ArrayList;
import java.util.List;

/** Converts IAM resources into commands and aggregates into resources. */
public final class IamResourceAssembler {

    private IamResourceAssembler() {
    }

    public static Result<SignUpCommand> toCommand(SignUpResource resource) {
        var roles = new ArrayList<UserRoleType>();
        for (var text : resource.roles() == null ? List.<String>of() : resource.roles()) {
            var role = EnumText.tryParse(UserRoleType.class, text);
            if (role.isEmpty()) return Result.failure(CommandError.validation("role-invalid"));
            roles.add(role.get());
        }
        return Result.success(new SignUpCommand(resource.email(), resource.password(), resource.fullName(),
                resource.dni(), resource.phone(), roles, toMerchantData(resource.merchant())));
    }

    public static Result<AddRoleCommand> toCommand(Long userId, AddRoleResource resource) {
        var role = EnumText.tryParse(UserRoleType.class, resource.role());
        if (role.isEmpty()) return Result.failure(CommandError.validation("role-invalid"));
        return Result.success(new AddRoleCommand(userId, role.get(), toMerchantData(resource.merchant())));
    }

    public static Result<DriverData> toDriverData(SaveDriverResource resource) {
        DriverSex sex = null;
        if (resource.sex() != null && !resource.sex().isBlank()) {
            var parsed = EnumText.tryParse(DriverSex.class, resource.sex());
            if (parsed.isEmpty()) return Result.failure(CommandError.validation("sex-invalid"));
            sex = parsed.get();
        }
        return Result.success(new DriverData(resource.fullName(), sex, resource.experienceStars(),
                resource.isActive() == null || resource.isActive()));
    }

    public static VehicleData toVehicleData(SaveVehicleResource resource) {
        return new VehicleData(resource.plate(), resource.model(), resource.maxLoadKg(), resource.cargoLengthM(),
                resource.cargoWidthM(), resource.cargoHeightM(), resource.cargoVolumeM3(), resource.photoS3Key(),
                resource.isActive() == null || resource.isActive());
    }

    public static AccountResource toResource(UserAccount account) {
        var user = account.user();
        var identity = account.identity();
        var merchant = account.merchantProfile();
        return new AccountResource(user.getPublicId(), user.getEmail(),
                user.roleTypes().stream().map(Enum::name).sorted().toList(),
                identity == null ? null : new IdentityResource(identity.getFullName(), "*****" + identity.getDniLast3(),
                        account.phone(), identity.getKycStatus().name()),
                account.carrierProfile() == null ? null : new CarrierProfileResource(account.carrierProfile().getId()),
                merchant == null ? null : new MerchantProfileResource(merchant.getId(), merchant.getBusinessName(),
                        merchant.getRuc(), merchant.getBusinessType()),
                user.getCreatedAt());
    }

    public static AuthenticatedSessionResource toResource(AuthenticatedSession session) {
        return new AuthenticatedSessionResource(session.accessToken(), session.accessTokenExpiresAt(),
                session.refreshToken(), session.refreshTokenExpiresAt(), toResource(session.account()));
    }

    public static IdentityVerificationResource toResource(IdentityVerification verification) {
        return new IdentityVerificationResource(verification.getId(), verification.getProvider(),
                verification.getStatus().name(), verification.getExternalReference(), verification.getRejectionReason(),
                verification.getSubmittedAt(), verification.getReviewedAt());
    }

    public static VehicleResource toResource(Vehicle vehicle) {
        return new VehicleResource(vehicle.getPublicId(), vehicle.getPlate(), vehicle.getModel(), vehicle.getMaxLoadKg(),
                vehicle.getCargoLengthM(), vehicle.getCargoWidthM(), vehicle.getCargoHeightM(),
                vehicle.getCargoVolumeM3(), vehicle.getPhotoS3Key(), vehicle.isActive());
    }

    public static DriverResource toResource(Driver driver) {
        return new DriverResource(driver.getId(), driver.getFullName(),
                driver.getSex() == null ? null : driver.getSex().name(), driver.getExperienceStars(), driver.isActive());
    }

    public static SavedAddressResource toResource(SavedAddress address) {
        return new SavedAddressResource(address.getId(), address.getLabel(), address.getAddress(), address.getDistrict(),
                address.getLocation().latitude(), address.getLocation().longitude());
    }

    private static MerchantData toMerchantData(MerchantResource resource) {
        return resource == null ? null
                : new MerchantData(resource.businessName(), resource.ruc(), resource.businessType());
    }
}
