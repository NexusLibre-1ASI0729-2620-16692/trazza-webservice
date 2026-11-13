package trazzaplatform.api.iam.application.internal.commandservices;

import trazzaplatform.api.iam.application.internal.outboundservices.IHashingService;
import trazzaplatform.api.iam.application.internal.outboundservices.IIdentityVerificationProvider;
import trazzaplatform.api.iam.application.internal.outboundservices.IPersonalDataProtector;
import trazzaplatform.api.iam.application.internal.outboundservices.ITokenService;
import trazzaplatform.api.iam.domain.model.aggregates.AuthenticatedSession;
import trazzaplatform.api.iam.domain.model.aggregates.CarrierProfile;
import trazzaplatform.api.iam.domain.model.aggregates.IdentityVerification;
import trazzaplatform.api.iam.domain.model.aggregates.MerchantProfile;
import trazzaplatform.api.iam.domain.model.aggregates.PersonIdentity;
import trazzaplatform.api.iam.domain.model.aggregates.RefreshToken;
import trazzaplatform.api.iam.domain.model.aggregates.User;
import trazzaplatform.api.iam.domain.model.aggregates.UserAccount;
import trazzaplatform.api.iam.domain.model.commands.AddRoleCommand;
import trazzaplatform.api.iam.domain.model.commands.MerchantData;
import trazzaplatform.api.iam.domain.model.commands.RefreshSessionCommand;
import trazzaplatform.api.iam.domain.model.commands.SignInCommand;
import trazzaplatform.api.iam.domain.model.commands.SignOutCommand;
import trazzaplatform.api.iam.domain.model.commands.SignUpCommand;
import trazzaplatform.api.iam.domain.model.commands.SubmitIdentityVerificationCommand;
import trazzaplatform.api.iam.domain.model.commands.UpdateIdentityCommand;
import trazzaplatform.api.iam.domain.model.commands.UpdateMerchantProfileCommand;
import trazzaplatform.api.iam.domain.model.valueobjects.Dni;
import trazzaplatform.api.iam.domain.model.valueobjects.EmailAddress;
import trazzaplatform.api.iam.domain.model.valueobjects.KycStatus;
import trazzaplatform.api.iam.domain.model.valueobjects.PhoneNumber;
import trazzaplatform.api.iam.domain.model.valueobjects.Ruc;
import trazzaplatform.api.iam.domain.model.valueobjects.UserRoleType;
import trazzaplatform.api.iam.domain.repositories.ICarrierProfileRepository;
import trazzaplatform.api.iam.domain.repositories.IIdentityVerificationRepository;
import trazzaplatform.api.iam.domain.repositories.IMerchantProfileRepository;
import trazzaplatform.api.iam.domain.repositories.IPersonIdentityRepository;
import trazzaplatform.api.iam.domain.repositories.IRefreshTokenRepository;
import trazzaplatform.api.iam.domain.repositories.IUserRepository;
import trazzaplatform.api.iam.domain.services.IUserCommandService;
import trazzaplatform.api.iam.domain.services.IUserQueryService;
import trazzaplatform.api.shared.domain.model.CommandError;
import trazzaplatform.api.shared.domain.model.Result;
import trazzaplatform.api.shared.domain.repositories.IUnitOfWork;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 *     Own authentication (no external identity provider): sign-up, sign-in with lockout, refresh token rotation,
 *     roles, person identity and simulated RENIEC validation.
 */
public class UserCommandService implements IUserCommandService {

    public static final int MIN_PASSWORD_LENGTH = 8;

    private final IUserRepository userRepository;
    private final IPersonIdentityRepository identityRepository;
    private final IIdentityVerificationRepository verificationRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final ICarrierProfileRepository carrierProfileRepository;
    private final IMerchantProfileRepository merchantProfileRepository;
    private final IUserQueryService userQueryService;
    private final IHashingService hashingService;
    private final ITokenService tokenService;
    private final IPersonalDataProtector dataProtector;
    private final IIdentityVerificationProvider verificationProvider;
    private final IUnitOfWork unitOfWork;
    private final Clock timeProvider;

    public UserCommandService(
            IUserRepository userRepository,
            IPersonIdentityRepository identityRepository,
            IIdentityVerificationRepository verificationRepository,
            IRefreshTokenRepository refreshTokenRepository,
            ICarrierProfileRepository carrierProfileRepository,
            IMerchantProfileRepository merchantProfileRepository,
            IUserQueryService userQueryService,
            IHashingService hashingService,
            ITokenService tokenService,
            IPersonalDataProtector dataProtector,
            IIdentityVerificationProvider verificationProvider,
            IUnitOfWork unitOfWork,
            Clock timeProvider) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.verificationRepository = verificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.carrierProfileRepository = carrierProfileRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.userQueryService = userQueryService;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.dataProtector = dataProtector;
        this.verificationProvider = verificationProvider;
        this.unitOfWork = unitOfWork;
        this.timeProvider = timeProvider;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(timeProvider.instant(), ZoneOffset.UTC);
    }

    // ----------------------------------------------------------------
    // Sign-up
    // ----------------------------------------------------------------

    public CompletionStage<Result<AuthenticatedSession, CommandError>> handle(SignUpCommand command) {
        var email = EmailAddress.create(command.getEmail());
        if (email.isFailure())
            return CompletableFuture.completedFuture(Result.failure(email.getError()));

        if ((command.getPassword() == null ? "" : command.getPassword()).length() < MIN_PASSWORD_LENGTH)
            return CompletableFuture.completedFuture(Result.failure(CommandError.validation("password-too-short")));

        var fullName = normalizeName(command.getFullName());
        if (fullName == null)
            return CompletableFuture.completedFuture(Result.failure(CommandError.validation("full-name-required")));

        var dni = Dni.create(command.getDni());
        if (dni.isFailure())
            return CompletableFuture.completedFuture(Result.failure(dni.getError()));

        var phone = PhoneNumber.create(command.getPhone());
        if (phone.isFailure())
            return CompletableFuture.completedFuture(Result.failure(phone.getError()));

        var roles = command.getRoles().stream().distinct().toList();
        if (roles.isEmpty())
            return CompletableFuture.completedFuture(Result.failure(CommandError.validation("role-required")));

        MerchantProfileData merchant = null;
        if (roles.contains(UserRoleType.MERCHANT)) {
            var merchantResult = validateMerchant(command.getMerchant());
            if (merchantResult.isFailure())
                return CompletableFuture.completedFuture(Result.failure(merchantResult.getError()));
            merchant = merchantResult.getValue();
        }

        final MerchantProfileData finalMerchant = merchant;
        final String finalFullName = fullName;
        final String finalPassword = command.getPassword();

        return userRepository.existsByEmailAsync(email.getValue().getValue())
                .thenCompose(emailExists -> {
                    if (emailExists)
                        return CompletableFuture.completedFuture(
                                Result.<AuthenticatedSession, CommandError>failure(
                                        CommandError.conflict("email-already-registered")));

                    var dniHash = dataProtector.hashDni(dni.getValue().getValue());
                    return identityRepository.existsByDniHashAsync(dniHash)
                            .thenCompose(dniExists -> {
                                if (dniExists)
                                    return CompletableFuture.completedFuture(
                                            Result.<AuthenticatedSession, CommandError>failure(
                                                    CommandError.conflict("dni-already-registered")));

                                if (finalMerchant != null) {
                                    return merchantProfileRepository.existsByRucAsync(finalMerchant.ruc())
                                            .thenCompose(rucExists -> {
                                                if (rucExists)
                                                    return CompletableFuture.completedFuture(
                                                            Result.<AuthenticatedSession, CommandError>failure(
                                                                    CommandError.conflict("ruc-already-registered")));
                                                return executeSignUpTransaction(email.getValue().getValue(),
                                                        finalPassword, finalFullName, dni, dniHash, phone,
                                                        roles, finalMerchant);
                                            });
                                }

                                return executeSignUpTransaction(email.getValue().getValue(), finalPassword,
                                        finalFullName, dni, dniHash, phone, roles, null);
                            });
                });
    }

    private CompletionStage<Result<AuthenticatedSession, CommandError>> executeSignUpTransaction(
            String emailValue,
            String password,
            String fullName,
            Dni dni,
            String dniHash,
            PhoneNumber phone,
            List<UserRoleType> roles,
            MerchantProfileData merchant) {

        return unitOfWork.executeInTransactionAsync(() -> {
            var now = now();
            var user = new User(emailValue, hashingService.hashPassword(password));
            for (var role : roles) user.grantRole(role, now);

            return userRepository.addAsync(user)
                    .thenCompose(u -> unitOfWork.completeAsync())
                    .thenCompose(u -> identityRepository.addAsync(new PersonIdentity(user.getId(), fullName,
                            dataProtector.encrypt(dni.getValue().getValue()), dniHash,
                            dni.getValue().getLastThree(),
                            dataProtector.encrypt(phone.getValue().getValue()))))
                    .thenCompose(v -> {
                        if (roles.contains(UserRoleType.CARRIER))
                            return carrierProfileRepository.addAsync(new CarrierProfile(user.getId()));
                        return CompletableFuture.completedFuture(null);
                    })
                    .thenCompose(v -> {
                        if (merchant != null)
                            return merchantProfileRepository.addAsync(new MerchantProfile(user.getId(),
                                    merchant.businessName(), merchant.ruc(), merchant.businessType()));
                        return CompletableFuture.completedFuture(null);
                    })
                    .thenCompose(v -> unitOfWork.completeAsync())
                    .thenCompose(v -> issueSessionAsync(user));
        }, Result::isSuccess);
    }

    // ----------------------------------------------------------------
    // Sign-in
    // ----------------------------------------------------------------

    public CompletionStage<Result<AuthenticatedSession, CommandError>> handle(SignInCommand command) {
        var email = EmailAddress.create(command.getEmail());
        if (email.isFailure() || command.getPassword() == null || command.getPassword().isEmpty())
            return CompletableFuture.completedFuture(Result.failure(CommandError.unauthorized("invalid-credentials")));

        return userRepository.findByEmailAsync(email.getValue().getValue())
                .thenCompose(user -> {
                    if (user == null || !user.canSignIn())
                        return CompletableFuture.completedFuture(
                                Result.<AuthenticatedSession, CommandError>failure(
                                        CommandError.unauthorized("invalid-credentials")));

                    if (user.isLocked(now()))
                        return CompletableFuture.completedFuture(
                                Result.<AuthenticatedSession, CommandError>failure(
                                        CommandError.forbidden("account-locked")));

                    if (!hashingService.verifyPassword(command.getPassword(), user.getPasswordHash())) {
                        user.registerFailedLogin(now());
                        return unitOfWork.completeAsync()
                                .thenApply(v -> Result.<AuthenticatedSession, CommandError>failure(
                                        CommandError.unauthorized("invalid-credentials")));
                    }

                    user.registerSuccessfulLogin(now());
                    return issueSessionAsync(user);
                });
    }

    // ----------------------------------------------------------------
    // Refresh session
    // ----------------------------------------------------------------

    public CompletionStage<Result<AuthenticatedSession, CommandError>> handle(RefreshSessionCommand command) {
        if (command.getRefreshToken() == null || command.getRefreshToken().isBlank())
            return CompletableFuture.completedFuture(
                    Result.failure(CommandError.unauthorized("invalid-refresh-token")));

        return refreshTokenRepository.findByHashAsync(tokenService.hashRefreshToken(command.getRefreshToken()))
                .thenCompose(stored -> {
                    if (stored == null || !stored.isActive(now()))
                        return CompletableFuture.completedFuture(
                                Result.<AuthenticatedSession, CommandError>failure(
                                        CommandError.unauthorized("invalid-refresh-token")));

                    return userRepository.findWithRolesAsync(stored.getUserId())
                            .thenCompose(user -> {
                                if (user == null || !user.canSignIn())
                                    return CompletableFuture.completedFuture(
                                            Result.<AuthenticatedSession, CommandError>failure(
                                                    CommandError.unauthorized("invalid-refresh-token")));

                                stored.revoke(now());
                                return issueSessionAsync(user);
                            });
                });
    }

    // ----------------------------------------------------------------
    // Sign-out
    // ----------------------------------------------------------------

    public CompletionStage<Result<Boolean, CommandError>> handle(SignOutCommand command) {
        if (command.getRefreshToken() == null || command.getRefreshToken().isBlank())
            return CompletableFuture.completedFuture(Result.success(true));

        return refreshTokenRepository.findByHashAsync(tokenService.hashRefreshToken(command.getRefreshToken()))
                .thenCompose(stored -> {
                    if (stored == null || !stored.getUserId().equals(command.getUserId()))
                        return CompletableFuture.completedFuture(Result.<Boolean, CommandError>success(true));

                    stored.revoke(now());
                    return unitOfWork.completeAsync()
                            .thenApply(v -> Result.<Boolean, CommandError>success(true));
                });
    }

    // ----------------------------------------------------------------
    // Update identity
    // ----------------------------------------------------------------

    public CompletionStage<Result<UserAccount, CommandError>> handle(UpdateIdentityCommand command) {
        var fullName = normalizeName(command.getFullName());
        if (fullName == null)
            return CompletableFuture.completedFuture(Result.failure(CommandError.validation("full-name-required")));

        var phone = PhoneNumber.create(command.getPhone());
        if (phone.isFailure())
            return CompletableFuture.completedFuture(Result.failure(phone.getError()));

        return identityRepository.findByUserIdAsync(command.getUserId())
                .thenCompose(identity -> {
                    if (identity == null)
                        return CompletableFuture.completedFuture(
                                Result.<UserAccount, CommandError>failure(
                                        CommandError.notFound("identity-not-found")));

                    identity.updateContact(fullName, dataProtector.encrypt(phone.getValue().getValue()));
                    return unitOfWork.completeAsync()
                            .thenCompose(v -> loadAccountAsync(command.getUserId()));
                });
    }

    // ----------------------------------------------------------------
    // Add role
    // ----------------------------------------------------------------

    public CompletionStage<Result<UserAccount, CommandError>> handle(AddRoleCommand command) {
        return userRepository.findWithRolesAsync(command.getUserId())
                .thenCompose(user -> {
                    if (user == null)
                        return CompletableFuture.completedFuture(
                                Result.<UserAccount, CommandError>failure(CommandError.notFound("user-not-found")));

                    if (user.hasRole(command.getRole()))
                        return CompletableFuture.completedFuture(
                                Result.<UserAccount, CommandError>failure(
                                        CommandError.conflict("role-already-granted")));

                    if (command.getRole() == UserRoleType.MERCHANT) {
                        var merchant = validateMerchant(command.getMerchant());
                        if (merchant.isFailure())
                            return CompletableFuture.completedFuture(
                                    Result.<UserAccount, CommandError>failure(merchant.getError()));

                        return merchantProfileRepository.existsByRucAsync(merchant.getValue().ruc())
                                .thenCompose(rucExists -> {
                                    if (rucExists)
                                        return CompletableFuture.completedFuture(
                                                Result.<UserAccount, CommandError>failure(
                                                        CommandError.conflict("ruc-already-registered")));

                                    return merchantProfileRepository.findByUserIdAsync(user.getId())
                                            .thenCompose(existing -> {
                                                if (existing == null)
                                                    return merchantProfileRepository.addAsync(
                                                            new MerchantProfile(user.getId(),
                                                                    merchant.getValue().businessName(),
                                                                    merchant.getValue().ruc(),
                                                                    merchant.getValue().businessType()));
                                                return CompletableFuture.completedFuture(existing);
                                            })
                                            .thenCompose(v -> grantRoleAndComplete(user, command.getRole()));
                                });
                    }

                    return carrierProfileRepository.findByUserIdAsync(user.getId())
                            .thenCompose(existing -> {
                                if (existing == null)
                                    return carrierProfileRepository.addAsync(new CarrierProfile(user.getId()));
                                return CompletableFuture.completedFuture(existing);
                            })
                            .thenCompose(v -> grantRoleAndComplete(user, command.getRole()));
                });
    }

    private CompletionStage<Result<UserAccount, CommandError>> grantRoleAndComplete(User user, UserRoleType role) {
        user.grantRole(role, now());
        return unitOfWork.completeAsync()
                .thenCompose(v -> loadAccountAsync(user.getId()));
    }

    // ----------------------------------------------------------------
    // Update merchant profile
    // ----------------------------------------------------------------

    public CompletionStage<Result<UserAccount, CommandError>> handle(UpdateMerchantProfileCommand command) {
        var businessName = (command.getBusinessName() == null ? "" : command.getBusinessName()).trim();
        var businessType = (command.getBusinessType() == null ? "" : command.getBusinessType()).trim();
        if (businessName.length() < 2 || businessName.length() > 255)
            return CompletableFuture.completedFuture(Result.failure(CommandError.validation("business-name-required")));
        if (businessType.length() < 2 || businessType.length() > 100)
            return CompletableFuture.completedFuture(Result.failure(CommandError.validation("business-type-required")));

        return merchantProfileRepository.findByUserIdAsync(command.getUserId())
                .thenCompose(profile -> {
                    if (profile == null)
                        return CompletableFuture.completedFuture(
                                Result.<UserAccount, CommandError>failure(
                                        CommandError.notFound("merchant-profile-not-found")));

                    profile.update(businessName, businessType);
                    return unitOfWork.completeAsync()
                            .thenCompose(v -> loadAccountAsync(command.getUserId()));
                });
    }

    // ----------------------------------------------------------------
    // Submit identity verification (KYC)
    // ----------------------------------------------------------------

    public CompletionStage<Result<IdentityVerification, CommandError>> handle(
            SubmitIdentityVerificationCommand command) {

        return identityRepository.findByUserIdAsync(command.getUserId())
                .thenCompose(identity -> {
                    if (identity == null)
                        return CompletableFuture.completedFuture(
                                Result.<IdentityVerification, CommandError>failure(
                                        CommandError.notFound("identity-not-found")));

                    if (identity.getKycStatus() == KycStatus.VALIDATED)
                        return CompletableFuture.completedFuture(
                                Result.<IdentityVerification, CommandError>failure(
                                        CommandError.conflict("identity-already-validated")));

                    var verification = new IdentityVerification(command.getUserId(),
                            blank(command.getDocumentFrontS3Key()),
                            blank(command.getDocumentBackS3Key()),
                            now());

                    return verificationProvider.verifyAsync(
                                    dataProtector.decrypt(identity.getDniEncrypted()),
                                    identity.getFullName())
                            .thenCompose(outcome -> {
                                if (outcome.approved()) {
                                    verification.validate(outcome.reference(), now());
                                    identity.setKycStatus(KycStatus.VALIDATED);
                                } else {
                                    verification.reject(
                                            outcome.reason() != null ? outcome.reason() : "not-validated", now());
                                    identity.setKycStatus(KycStatus.REJECTED);
                                }

                                return verificationRepository.addAsync(verification)
                                        .thenCompose(v -> unitOfWork.completeAsync())
                                        .thenApply(v -> Result.<IdentityVerification, CommandError>success(
                                                verification));
                            });
                });
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private CompletionStage<Result<AuthenticatedSession, CommandError>> issueSessionAsync(User user) {
        var access = tokenService.generateAccessToken(user);
        var refresh = tokenService.generateRefreshToken();

        return refreshTokenRepository.addAsync(new RefreshToken(user.getId(), refresh.hash(),
                        refresh.expiresAt(), now()))
                .thenCompose(v -> unitOfWork.completeAsync())
                .thenCompose(v -> userQueryService.getAccountAsync(user.getId()))
                .thenApply(account -> {
                    if (account == null)
                        return Result.<AuthenticatedSession, CommandError>failure(
                                CommandError.notFound("user-not-found"));

                    return Result.<AuthenticatedSession, CommandError>success(
                            new AuthenticatedSession(account, access.token(), access.expiresAt(),
                                    refresh.token(), refresh.expiresAt()));
                });
    }

    private CompletionStage<Result<UserAccount, CommandError>> loadAccountAsync(Long userId) {
        return userQueryService.getAccountAsync(userId)
                .thenApply(account -> {
                    if (account == null)
                        return Result.<UserAccount, CommandError>failure(
                                CommandError.notFound("user-not-found"));
                    return Result.<UserAccount, CommandError>success(account);
                });
    }

    private static Result<MerchantProfileData, CommandError> validateMerchant(MerchantData data) {
        if (data == null)
            return Result.failure(CommandError.validation("merchant-data-required"));

        var businessName = (data.getBusinessName() == null ? "" : data.getBusinessName()).trim();
        if (businessName.length() < 2 || businessName.length() > 255)
            return Result.failure(CommandError.validation("business-name-required"));

        var businessType = (data.getBusinessType() == null ? "" : data.getBusinessType()).trim();
        if (businessType.length() < 2 || businessType.length() > 100)
            return Result.failure(CommandError.validation("business-type-required"));

        var ruc = Ruc.create(data.getRuc());
        if (ruc.isFailure())
            return Result.failure(ruc.getError());

        return Result.success(new MerchantProfileData(businessName, ruc.getValue().getValue(), businessType));
    }

    private static String normalizeName(String fullName) {
        var normalized = (fullName == null ? "" : fullName).trim();
        return (normalized.length() < 3 || normalized.length() > 200) ? null : normalized;
    }

    private static String blank(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private record MerchantProfileData(String businessName, String ruc, String businessType) {}
}