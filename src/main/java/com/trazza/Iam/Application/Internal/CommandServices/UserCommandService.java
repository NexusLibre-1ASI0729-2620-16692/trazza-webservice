package com.trazza.iam.application.internal.commandservices;

import com.trazza.iam.application.internal.outboundservices.IHashingService;
import com.trazza.iam.application.internal.outboundservices.IIdentityVerificationProvider;
import com.trazza.iam.application.internal.outboundservices.IPersonalDataProtector;
import com.trazza.iam.application.internal.outboundservices.ITokenService;
import com.trazza.iam.domain.model.aggregates.*;
import com.trazza.iam.domain.model.commands.*;
import com.trazza.iam.domain.model.valueobjects.*;
import com.trazza.iam.domain.repositories.*;
import com.trazza.iam.domain.services.IUserCommandService;
import com.trazza.iam.domain.services.IUserQueryService;
import com.trazza.shared.domain.model.CommandError;
import com.trazza.shared.domain.model.Result;
import com.trazza.shared.domain.repositories.IUnitOfWork;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Own authentication (no external identity provider): sign-up, sign-in with lockout, refresh token rotation,
 * roles, person identity and simulated RENIEC validation.
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

    @Override
    public Result<AuthenticatedSession, CommandError> handle(SignUpCommand command) {
        var email = EmailAddress.create(command.getEmail());
        if (email.isFailure()) return Result.failure(email.getError());

        if ((command.getPassword() == null ? "" : command.getPassword()).length() < MIN_PASSWORD_LENGTH)
            return Result.failure(CommandError.validation("password-too-short"));

        var fullName = normalizeName(command.getFullName());
        if (fullName == null) return Result.failure(CommandError.validation("full-name-required"));

        var dni = Dni.create(command.getDni());
        if (dni.isFailure()) return Result.failure(dni.getError());

        var phone = PhoneNumber.create(command.getPhone());
        if (phone.isFailure()) return Result.failure(phone.getError());

        var roles = command.getRoles().stream().distinct().toList();
        if (roles.isEmpty()) return Result.failure(CommandError.validation("role-required"));

        MerchantProfileData merchant = null;
        if (roles.contains(UserRoleType.MERCHANT)) {
            var merchantResult = validateMerchant(command.getMerchant());
            if (merchantResult.isFailure()) return Result.failure(merchantResult.getError());
            merchant = merchantResult.getValue();
        }

        if (userRepository.existsByEmail(email.getValue().getValue()))
            return Result.failure(CommandError.conflict("email-already-registered"));

        var dniHash = dataProtector.hashDni(dni.getValue().getValue());
        if (identityRepository.existsByDniHash(dniHash))
            return Result.failure(CommandError.conflict("dni-already-registered"));

        if (merchant != null && merchantProfileRepository.existsByRuc(merchant.ruc()))
            return Result.failure(CommandError.conflict("ruc-already-registered"));

        // Ejecutamos la transacción síncrona
        try {
            unitOfWork.beginTransaction(); // Asumiendo que tienes este método en tu IUnitOfWork

            var now = now();
            var user = new User(email.getValue().getValue(), hashingService.hashPassword(command.getPassword()));
            for (var role : roles) user.grantRole(role, now);

            userRepository.add(user);
            unitOfWork.complete();

            identityRepository.add(new PersonIdentity(user.getId(), fullName,
                    dataProtector.encrypt(dni.getValue().getValue()), dniHash, dni.getValue().getLastThree(),
                    dataProtector.encrypt(phone.getValue().getValue())));

            if (roles.contains(UserRoleType.CARRIER)) {
                carrierProfileRepository.add(new CarrierProfile(user.getId()));
            }

            if (merchant != null) {
                merchantProfileRepository.add(new MerchantProfile(user.getId(), merchant.businessName(), merchant.ruc(), merchant.businessType()));
            }

            unitOfWork.complete();

            return issueSession(user);

        } catch (Exception e) {
            unitOfWork.rollbackTransaction(); // Asumiendo que tienes este método
            throw new RuntimeException("Error during sign up", e);
        }
    }

    @Override
    public Result<AuthenticatedSession, CommandError> handle(SignInCommand command) {
        var email = EmailAddress.create(command.getEmail());
        if (email.isFailure() || command.getPassword() == null || command.getPassword().isEmpty())
            return Result.failure(CommandError.unauthorized("invalid-credentials"));

        var user = userRepository.findByEmail(email.getValue().getValue());
        if (user == null || !user.canSignIn()) return Result.failure(CommandError.unauthorized("invalid-credentials"));

        if (user.isLocked(now())) return Result.failure(CommandError.forbidden("account-locked"));

        if (!hashingService.verifyPassword(command.getPassword(), user.getPasswordHash())) {
            user.registerFailedLogin(now());
            unitOfWork.complete();
            return Result.failure(CommandError.unauthorized("invalid-credentials"));
        }

        user.registerSuccessfulLogin(now());
        return issueSession(user);
    }

    @Override
    public Result<AuthenticatedSession, CommandError> handle(RefreshSessionCommand command) {
        if (command.getRefreshToken() == null || command.getRefreshToken().isBlank())
            return Result.failure(CommandError.unauthorized("invalid-refresh-token"));

        var stored = refreshTokenRepository.findByHash(tokenService.hashRefreshToken(command.getRefreshToken()));
        if (stored == null || !stored.isActive(now())) return Result.failure(CommandError.unauthorized("invalid-refresh-token"));

        var user = userRepository.findWithRoles(stored.getUserId());
        if (user == null || !user.canSignIn()) return Result.failure(CommandError.unauthorized("invalid-refresh-token"));

        stored.revoke(now());
        return issueSession(user);
    }

    @Override
    public Result<Boolean, CommandError> handle(SignOutCommand command) {
        if (command.getRefreshToken() == null || command.getRefreshToken().isBlank()) return Result.success(true);

        var stored = refreshTokenRepository.findByHash(tokenService.hashRefreshToken(command.getRefreshToken()));
        if (stored == null || !stored.getUserId().equals(command.getUserId())) return Result.success(true);

        stored.revoke(now());
        unitOfWork.complete();
        return Result.success(true);
    }

    @Override
    public Result<UserAccount, CommandError> handle(UpdateIdentityCommand command) {
        var fullName = normalizeName(command.getFullName());
        if (fullName == null) return Result.failure(CommandError.validation("full-name-required"));

        var phone = PhoneNumber.create(command.getPhone());
        if (phone.isFailure()) return Result.failure(phone.getError());

        var identity = identityRepository.findByUserId(command.getUserId());
        if (identity == null) return Result.failure(CommandError.notFound("identity-not-found"));

        identity.updateContact(fullName, dataProtector.encrypt(phone.getValue().getValue()));
        unitOfWork.complete();
        return loadAccount(command.getUserId());
    }

    @Override
    public Result<UserAccount, CommandError> handle(AddRoleCommand command) {
        var user = userRepository.findWithRoles(command.getUserId());
        if (user == null) return Result.failure(CommandError.notFound("user-not-found"));
        if (user.hasRole(command.getRole())) return Result.failure(CommandError.conflict("role-already-granted"));

        if (command.getRole() == UserRoleType.MERCHANT) {
            var merchant = validateMerchant(command.getMerchant());
            if (merchant.isFailure()) return Result.failure(merchant.getError());

            if (merchantProfileRepository.existsByRuc(merchant.getValue().ruc()))
                return Result.failure(CommandError.conflict("ruc-already-registered"));

            if (merchantProfileRepository.findByUserId(user.getId()) == null)
                merchantProfileRepository.add(new MerchantProfile(user.getId(), merchant.getValue().businessName(),
                        merchant.getValue().ruc(), merchant.getValue().businessType()));

        } else if (carrierProfileRepository.findByUserId(user.getId()) == null) {
            carrierProfileRepository.add(new CarrierProfile(user.getId()));
        }

        user.grantRole(command.getRole(), now());
        unitOfWork.complete();
        return loadAccount(user.getId());
    }

    @Override
    public Result<UserAccount, CommandError> handle(UpdateMerchantProfileCommand command) {
        var businessName = (command.getBusinessName() == null ? "" : command.getBusinessName()).trim();
        var businessType = (command.getBusinessType() == null ? "" : command.getBusinessType()).trim();

        if (businessName.length() < 2 || businessName.length() > 255) return Result.failure(CommandError.validation("business-name-required"));
        if (businessType.length() < 2 || businessType.length() > 100) return Result.failure(CommandError.validation("business-type-required"));

        var profile = merchantProfileRepository.findByUserId(command.getUserId());
        if (profile == null) return Result.failure(CommandError.notFound("merchant-profile-not-found"));

        profile.update(businessName, businessType);
        unitOfWork.complete();
        return loadAccount(command.getUserId());
    }

    @Override
    public Result<IdentityVerification, CommandError> handle(SubmitIdentityVerificationCommand command) {
        var identity = identityRepository.findByUserId(command.getUserId());
        if (identity == null) return Result.failure(CommandError.notFound("identity-not-found"));
        if (identity.getKycStatus() == KycStatus.VALIDATED) return Result.failure(CommandError.conflict("identity-already-validated"));

        var verification = new IdentityVerification(command.getUserId(), blank(command.getDocumentFrontS3Key()),
                blank(command.getDocumentBackS3Key()), now());

        var outcome = verificationProvider.verifyAsync(dataProtector.decrypt(identity.getDniEncrypted()), identity.getFullName()).join();

        if (outcome.approved()) {
            verification.validate(outcome.reference(), now());
            identity.setKycStatus(KycStatus.VALIDATED);
        } else {
            verification.reject(outcome.reason() != null ? outcome.reason() : "not-validated", now());
            identity.setKycStatus(KycStatus.REJECTED);
        }

        verificationRepository.add(verification);
        unitOfWork.complete();
        return Result.success(verification);
    }

    private Result<AuthenticatedSession, CommandError> issueSession(User user) {
        var access = tokenService.generateAccessToken(user);
        var refresh = tokenService.generateRefreshToken();

        refreshTokenRepository.add(new RefreshToken(user.getId(), refresh.hash(), refresh.expiresAt(), now()));
        unitOfWork.complete();

        var accountResult = userQueryService.getAccountAsync(user.getId());
        // Si getAccountAsync devuelve un CompletionStage, tendrías que hacer .join() o cambiar el query service a síncrono.
        // Aquí asumo que es síncrono o que hiciste join.
        var account = (accountResult != null) ? accountResult.join() : null;

        if (account == null) return Result.failure(CommandError.notFound("user-not-found"));

        return Result.success(new AuthenticatedSession(account, access.token(), access.expiresAt(), refresh.token(), refresh.expiresAt()));
    }

    private Result<UserAccount, CommandError> loadAccount(Long userId) {
        var accountStage = userQueryService.getAccountAsync(userId);
        var account = (accountStage != null) ? accountStage.join() : null;

        if (account == null) return Result.failure(CommandError.notFound("user-not-found"));
        return Result.success(account);
    }

    private static Result<MerchantProfileData, CommandError> validateMerchant(MerchantData data) {
        if (data == null) return Result.failure(CommandError.validation("merchant-data-required"));

        var businessName = (data.getBusinessName() == null ? "" : data.getBusinessName()).trim();
        if (businessName.length() < 2 || businessName.length() > 255) return Result.failure(CommandError.validation("business-name-required"));

        var businessType = (data.getBusinessType() == null ? "" : data.getBusinessType()).trim();
        if (businessType.length() < 2 || businessType.length() > 100) return Result.failure(CommandError.validation("business-type-required"));

        var ruc = Ruc.create(data.getRuc());
        if (ruc.isFailure()) return Result.failure(ruc.getError());

        return Result.success(new MerchantProfileData(businessName, ruc.getValue().getValue(), businessType));
    }

    private static String normalizeName(String fullName) {
        var normalized = (fullName == null ? "" : fullName).trim();
        return (normalized.length() < 3 || normalized.length() > 200) ? null : normalized;
    }

    private static String blank(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    // Clase interna record para reemplazar el record anidado de C#
    private record MerchantProfileData(String businessName, String ruc, String businessType) {}
}