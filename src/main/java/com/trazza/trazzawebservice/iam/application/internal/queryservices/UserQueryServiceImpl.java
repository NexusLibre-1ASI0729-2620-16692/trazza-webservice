package com.trazza.trazzawebservice.iam.application.internal.queryservices;

import com.trazza.trazzawebservice.iam.application.internal.outboundservices.security.PersonalDataProtector;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.IdentityVerification;
import com.trazza.trazzawebservice.iam.domain.services.UserAccount;
import com.trazza.trazzawebservice.iam.domain.services.UserQueryService;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.CarrierProfileRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.IdentityVerificationRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.MerchantProfileRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.PersonIdentityRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;
    private final PersonIdentityRepository identityRepository;
    private final IdentityVerificationRepository verificationRepository;
    private final CarrierProfileRepository carrierProfileRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final PersonalDataProtector dataProtector;

    public UserQueryServiceImpl(UserRepository userRepository, PersonIdentityRepository identityRepository,
                                IdentityVerificationRepository verificationRepository,
                                CarrierProfileRepository carrierProfileRepository,
                                MerchantProfileRepository merchantProfileRepository, PersonalDataProtector dataProtector) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.verificationRepository = verificationRepository;
        this.carrierProfileRepository = carrierProfileRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.dataProtector = dataProtector;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> getAccount(Long userId) {
        return userRepository.findById(userId).map(user -> {
            var identity = identityRepository.findById(userId).orElse(null);
            var phone = identity == null ? null : dataProtector.decrypt(identity.getPhoneEncrypted());
            return new UserAccount(user, identity, phone, carrierProfileRepository.findByUserId(userId).orElse(null),
                    merchantProfileRepository.findByUserId(userId).orElse(null));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityVerification> listVerifications(Long userId) {
        return verificationRepository.findByUserIdOrderBySubmittedAtDesc(userId);
    }
}
