package com.trazza.iam.application.internal.queryservices;

import com.trazza.iam.application.internal.outboundservices.IPersonalDataProtector;
import com.trazza.iam.domain.model.aggregates.IdentityVerification;
import com.trazza.iam.domain.model.aggregates.UserAccount;
import com.trazza.iam.domain.repositories.ICarrierProfileRepository;
import com.trazza.iam.domain.repositories.IIdentityVerificationRepository;
import com.trazza.iam.domain.repositories.IMerchantProfileRepository;
import com.trazza.iam.domain.repositories.IPersonIdentityRepository;
import com.trazza.iam.domain.repositories.IUserRepository;
import com.trazza.iam.domain.services.IUserQueryService;

import java.util.List;

public class UserQueryService implements IUserQueryService {

    private final IUserRepository userRepository;
    private final IPersonIdentityRepository identityRepository;
    private final IIdentityVerificationRepository verificationRepository;
    private final ICarrierProfileRepository carrierProfileRepository;
    private final IMerchantProfileRepository merchantProfileRepository;
    private final IPersonalDataProtector dataProtector;

    public UserQueryService(
            IUserRepository userRepository,
            IPersonIdentityRepository identityRepository,
            IIdentityVerificationRepository verificationRepository,
            ICarrierProfileRepository carrierProfileRepository,
            IMerchantProfileRepository merchantProfileRepository,
            IPersonalDataProtector dataProtector) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.verificationRepository = verificationRepository;
        this.carrierProfileRepository = carrierProfileRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.dataProtector = dataProtector;
    }

    @Override
    public UserAccount getAccount(Long userId) {
        var user = userRepository.findWithRoles(userId);
        if (user == null) return null;

        var identity = identityRepository.findByUserId(userId);
        var phone = (identity == null) ? null : dataProtector.decrypt(identity.getPhoneEncrypted());
        var carrier = carrierProfileRepository.findByUserId(userId);
        var merchant = merchantProfileRepository.findByUserId(userId);

        return new UserAccount(user, identity, phone, carrier, merchant);
    }

    @Override
    public List<IdentityVerification> listVerifications(Long userId) {
        return verificationRepository.listByUser(userId);
    }
}