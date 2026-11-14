package com.trazza.iam.domain.services;

import com.trazza.iam.domain.model.aggregates.*;
import com.trazza.iam.domain.model.commands.*;
import com.trazza.shared.domain.model.CommandError;
import com.trazza.shared.domain.model.Result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class IamServices {

    private IamServices() {
        // Clase contenedora, no instanciable
    }

    /**
     * Account as seen by its owner: user, decrypted contact data and profiles.
     */
    public record UserAccount(
            User user,
            PersonIdentity identity,
            String phone,
            CarrierProfile carrierProfile,
            MerchantProfile merchantProfile
    ) {}

    /**
     * Result of sign-up, sign-in and refresh.
     */
    public record AuthenticatedSession(
            UserAccount account,
            String accessToken,
            LocalDateTime accessTokenExpiresAt,
            String refreshToken,
            LocalDateTime refreshTokenExpiresAt
    ) {}

    public interface UserCommandService {
        CompletableFuture<Result<AuthenticatedSession, CommandError>> handle(SignUpCommand command);
        CompletableFuture<Result<AuthenticatedSession, CommandError>> handle(SignInCommand command);
        CompletableFuture<Result<AuthenticatedSession, CommandError>> handle(RefreshSessionCommand command);
        CompletableFuture<Result<Boolean, CommandError>> handle(SignOutCommand command);
        CompletableFuture<Result<UserAccount, CommandError>> handle(UpdateIdentityCommand command);
        CompletableFuture<Result<UserAccount, CommandError>> handle(AddRoleCommand command);
        CompletableFuture<Result<UserAccount, CommandError>> handle(UpdateMerchantProfileCommand command);
        CompletableFuture<Result<IdentityVerification, CommandError>> handle(SubmitIdentityVerificationCommand command);
    }

    public interface UserQueryService {
        CompletableFuture<UserAccount> getAccount(Long userId);
        CompletableFuture<List<IdentityVerification>> listVerifications(Long userId);
    }

    public interface FleetCommandService {
        CompletableFuture<Result<Vehicle, CommandError>> handle(CreateVehicleCommand command);
        CompletableFuture<Result<Vehicle, CommandError>> handle(UpdateVehicleCommand command);
        CompletableFuture<Result<Vehicle, CommandError>> handle(DeactivateVehicleCommand command);
        CompletableFuture<Result<Driver, CommandError>> handle(CreateDriverCommand command);
        CompletableFuture<Result<Driver, CommandError>> handle(UpdateDriverCommand command);
        CompletableFuture<Result<Driver, CommandError>> handle(DeactivateDriverCommand command);
        CompletableFuture<Result<SavedAddress, CommandError>> handle(CreateSavedAddressCommand command);
        CompletableFuture<Result<Boolean, CommandError>> handle(DeleteSavedAddressCommand command);
    }

    public interface FleetQueryService {
        CompletableFuture<List<Vehicle>> listVehicles(Long userId);
        CompletableFuture<List<Driver>> listDrivers(Long userId);
        CompletableFuture<List<SavedAddress>> listSavedAddresses(Long userId);
    }
}