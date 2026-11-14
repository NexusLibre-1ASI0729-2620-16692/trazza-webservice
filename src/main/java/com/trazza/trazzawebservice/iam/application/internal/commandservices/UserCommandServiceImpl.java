package com.trazza.trazzawebservice.iam.application.internal.commandservices;

import com.trazza.trazzawebservice.iam.application.internal.outboundservices.hashing.HashingService;
import com.trazza.trazzawebservice.iam.application.internal.outboundservices.security.IdentityVerificationProvider;
import com.trazza.trazzawebservice.iam.application.internal.outboundservices.security.PersonalDataProtector;
import com.trazza.trazzawebservice.iam.application.internal.outboundservices.tokens.TokenService;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.CarrierProfile;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.IdentityVerification;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.MerchantProfile;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.PersonIdentity;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.RefreshToken;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.User;
import com.trazza.trazzawebservice.iam.domain.model.commands.AddRoleCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.MerchantData;
import com.trazza.trazzawebservice.iam.domain.model.commands.RefreshSessionCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.SignInCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.SignOutCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.SignUpCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.SubmitIdentityVerificationCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateIdentityCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateMerchantProfileCommand;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.Dni;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.EmailAddress;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.KycStatus;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.Ruc;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.UserRoleType;
import com.trazza.trazzawebservice.iam.domain.services.AuthenticatedSession;
import com.trazza.trazzawebservice.iam.domain.services.UserAccount;
import com.trazza.trazzawebservice.iam.domain.services.UserCommandService;
import com.trazza.trazzawebservice.iam.domain.services.UserQueryService;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.CarrierProfileRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.IdentityVerificationRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.MerchantProfileRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.PersonIdentityRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.RefreshTokenRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.PhoneNumber;
import com.trazza.trazzawebservice.shared.infrastructure.transactions.Transactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Own authentication (no external identity provider): sign-up, sign-in with lockout, refresh token rotation, roles,
 * person identity and simulated RENIEC validation.
 */
@Service
public class UserCommandServiceImpl implements UserCommandService {

    public static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PersonIdentityRepository identityRepository;
    private final IdentityVerificationRepository verificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CarrierProfileRepository carrierProfileRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final UserQueryService userQueryService;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final PersonalDataProtector dataProtector;
    private final IdentityVerificationProvider verificationProvider;

    public UserCommandServiceImpl(UserRepository userRepository, PersonIdentityRepository identityRepository,
                                  IdentityVerificationRepository verificationRepository,
                                  RefreshTokenRepository refreshTokenRepository,
                                  CarrierProfileRepository carrierProfileRepository,
                                  MerchantProfileRepository merchantProfileRepository, UserQueryService userQueryService,
                                  HashingService hashingService, TokenService tokenService,
                                  PersonalDataProtector dataProtector,
                                  IdentityVerificationProvider verificationProvider) {
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
    }

    @Override
    @Transactional
    public Result<AuthenticatedSession> handle(SignUpCommand command) {
        var email = EmailAddress.create(command.email());
        if (email.isFailure()) return email.asFailure();
        if (command.password() == null || command.password().length() < MIN_PASSWORD_LENGTH)
            return Result.failure(CommandError.validation("password-too-short"));
        var fullName = normalizeName(command.fullName());
        if (fullName == null) return Result.failure(CommandError.validation("full-name-required"));
        var dni = Dni.create(command.dni());
        if (dni.isFailure()) return dni.asFailure();
        var phone = PhoneNumber.create(command.phone());
        if (phone.isFailure()) return phone.asFailure();
        var roles = command.roles() == null ? List.<UserRoleType>of() : command.roles().stream().distinct().toList();
        if (roles.isEmpty()) return Result.failure(CommandError.validation("role-required"));

        MerchantData merchant = null;
        if (roles.contains(UserRoleType.MERCHANT)) {
            var merchantResult = validateMerchant(command.merchant());
            if (merchantResult.isFailure()) return merchantResult.asFailure();
            merchant = merchantResult.value();
        }

        if (userRepository.existsByEmail(email.value().value()))
            return Result.failure(CommandError.conflict("email-already-registered"));
        var dniHash = dataProtector.hashDni(dni.value().value());
        if (identityRepository.existsByDniHash(dniHash))
            return Result.failure(CommandError.conflict("dni-already-registered"));
        if (merchant != null && merchantProfileRepository.existsByRuc(merchant.ruc()))
            return Result.failure(CommandError.conflict("ruc-already-registered"));

        var now = Instant.now();
        var user = new User(email.value().value(), hashingService.hashPassword(command.password()));
        roles.forEach(role -> user.grantRole(role, now));
        userRepository.saveAndFlush(user);

        identityRepository.save(new PersonIdentity(user.getId(), fullName, dataProtector.encrypt(dni.value().value()),
                dniHash, dni.value().lastThree(), dataProtector.encrypt(phone.value().value())));
        if (roles.contains(UserRoleType.CARRIER)) carrierProfileRepository.save(new CarrierProfile(user.getId()));
        if (merchant != null)
            merchantProfileRepository.save(new MerchantProfile(user.getId(), merchant.businessName(), merchant.ruc(),
                    merchant.businessType()));
        return Transactions.rollbackOnFailure(issueSession(user));
    }

    @Override
    @Transactional
    public Result<AuthenticatedSession> handle(SignInCommand command) {
        var email = EmailAddress.create(command.email());
        if (email.isFailure() || command.password() == null || command.password().isEmpty())
            return Result.failure(CommandError.unauthorized("invalid-credentials"));

        var found = userRepository.findByEmail(email.value().value());
        if (found.isEmpty() || !found.get().canSignIn())
            return Result.failure(CommandError.unauthorized("invalid-credentials"));
        var user = found.get();
        var now = Instant.now();
        if (user.isLocked(now)) return Result.failure(CommandError.forbidden("account-locked"));

        if (!hashingService.verifyPassword(command.password(), user.getPasswordHash())) {
            user.registerFailedLogin(now);
            userRepository.save(user);
            return Result.failure(CommandError.unauthorized("invalid-credentials"));
        }

        user.registerSuccessfulLogin(now);
        userRepository.save(user);
        return issueSession(user);
    }

    @Override
    @Transactional
    public Result<AuthenticatedSession> handle(RefreshSessionCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank())
            return Result.failure(CommandError.unauthorized("invalid-refresh-token"));
        var now = Instant.now();
        var stored = refreshTokenRepository.findByTokenHash(tokenService.hashRefreshToken(command.refreshToken()));
        if (stored.isEmpty() || !stored.get().isActive(now))
            return Result.failure(CommandError.unauthorized("invalid-refresh-token"));

        var user = userRepository.findById(stored.get().getUserId());
        if (user.isEmpty() || !user.get().canSignIn())
            return Result.failure(CommandError.unauthorized("invalid-refresh-token"));

        stored.get().revoke(now);
        refreshTokenRepository.save(stored.get());
        return issueSession(user.get());
    }

    @Override
    @Transactional
    public Result<Boolean> handle(SignOutCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) return Result.success(true);
        refreshTokenRepository.findByTokenHash(tokenService.hashRefreshToken(command.refreshToken()))
                .filter(stored -> Objects.equals(stored.getUserId(), command.userId()))
                .ifPresent(stored -> {
                    stored.revoke(Instant.now());
                    refreshTokenRepository.save(stored);
                });
        return Result.success(true);
    }

    @Override
    @Transactional
    public Result<UserAccount> handle(UpdateIdentityCommand command) {
        var fullName = normalizeName(command.fullName());
        if (fullName == null) return Result.failure(CommandError.validation("full-name-required"));
        var phone = PhoneNumber.create(command.phone());
        if (phone.isFailure()) return phone.asFailure();

        var identity = identityRepository.findById(command.userId());
        if (identity.isEmpty()) return Result.failure(CommandError.notFound("identity-not-found"));
        identity.get().updateContact(fullName, dataProtector.encrypt(phone.value().value()));
        identityRepository.saveAndFlush(identity.get());
        return loadAccount(command.userId());
    }

    @Override
    @Transactional
    public Result<UserAccount> handle(AddRoleCommand command) {
        var found = userRepository.findById(command.userId());
        if (found.isEmpty()) return Result.failure(CommandError.notFound("user-not-found"));
        var user = found.get();
        if (user.hasRole(command.role())) return Result.failure(CommandError.conflict("role-already-granted"));

        if (command.role() == UserRoleType.MERCHANT) {
            var merchant = validateMerchant(command.merchant());
            if (merchant.isFailure()) return merchant.asFailure();
            if (merchantProfileRepository.existsByRuc(merchant.value().ruc()))
                return Result.failure(CommandError.conflict("ruc-already-registered"));
            if (merchantProfileRepository.findByUserId(user.getId()).isEmpty())
                merchantProfileRepository.save(new MerchantProfile(user.getId(), merchant.value().businessName(),
                        merchant.value().ruc(), merchant.value().businessType()));
        } else if (carrierProfileRepository.findByUserId(user.getId()).isEmpty()) {
            carrierProfileRepository.save(new CarrierProfile(user.getId()));
        }

        user.grantRole(command.role(), Instant.now());
        userRepository.saveAndFlush(user);
        return loadAccount(user.getId());
    }

    @Override
    @Transactional
    public Result<UserAccount> handle(UpdateMerchantProfileCommand command) {
        var businessName = trim(command.businessName());
        var businessType = trim(command.businessType());
        if (businessName.length() < 2 || businessName.length() > 255)
            return Result.failure(CommandError.validation("business-name-required"));
        if (businessType.length() < 2 || businessType.length() > 100)
            return Result.failure(CommandError.validation("business-type-required"));

        var profile = merchantProfileRepository.findByUserId(command.userId());
        if (profile.isEmpty()) return Result.failure(CommandError.notFound("merchant-profile-not-found"));
        profile.get().update(businessName, businessType);
        merchantProfileRepository.saveAndFlush(profile.get());
        return loadAccount(command.userId());
    }

    @Override
    @Transactional
    public Result<IdentityVerification> handle(SubmitIdentityVerificationCommand command) {
        var found = identityRepository.findById(command.userId());
        if (found.isEmpty()) return Result.failure(CommandError.notFound("identity-not-found"));
        var identity = found.get();
        if (identity.getKycStatus() == KycStatus.VALIDATED)
            return Result.failure(CommandError.conflict("identity-already-validated"));

        var now = Instant.now();
        var verification = new IdentityVerification(command.userId(), blank(command.documentFrontS3Key()),
                blank(command.documentBackS3Key()), now);
        var outcome = verificationProvider.verify(dataProtector.decrypt(identity.getDniEncrypted()),
                identity.getFullName());
        if (outcome.approved()) {
            verification.validate(outcome.reference(), now);
            identity.setKycStatus(KycStatus.VALIDATED);
        } else {
            verification.reject(outcome.reason() == null ? "not-validated" : outcome.reason(), now);
            identity.setKycStatus(KycStatus.REJECTED);
        }

        identityRepository.save(identity);
        return Result.success(verificationRepository.save(verification));
    }

    private Result<AuthenticatedSession> issueSession(User user) {
        var access = tokenService.generateAccessToken(user);
        var refresh = tokenService.generateRefreshToken();
        refreshTokenRepository.saveAndFlush(new RefreshToken(user.getId(), refresh.hash(), refresh.expiresAt(),
                Instant.now()));

        var account = userQueryService.getAccount(user.getId());
        if (account.isEmpty()) return Result.failure(CommandError.notFound("user-not-found"));
        return Result.success(new AuthenticatedSession(account.get(), access.token(), access.expiresAt(),
                refresh.token(), refresh.expiresAt()));
    }

    private Result<UserAccount> loadAccount(Long userId) {
        return userQueryService.getAccount(userId)
                .map(Result::success)
                .orElseGet(() -> Result.failure(CommandError.notFound("user-not-found")));
    }

    private static Result<MerchantData> validateMerchant(MerchantData data) {
        if (data == null) return Result.failure(CommandError.validation("merchant-data-required"));
        var businessName = trim(data.businessName());
        if (businessName.length() < 2 || businessName.length() > 255)
            return Result.failure(CommandError.validation("business-name-required"));
        var businessType = trim(data.businessType());
        if (businessType.length() < 2 || businessType.length() > 100)
            return Result.failure(CommandError.validation("business-type-required"));
        var ruc = Ruc.create(data.ruc());
        if (ruc.isFailure()) return ruc.asFailure();
        return Result.success(new MerchantData(businessName, ruc.value().value(), businessType));
    }

    private static String normalizeName(String fullName) {
        var normalized = trim(fullName);
        return normalized.length() < 3 || normalized.length() > 200 ? null : normalized;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
