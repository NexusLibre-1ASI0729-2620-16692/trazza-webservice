package trazzaplatform.api.iam.application.internal.queryservices;

import trazzaplatform.api.iam.application.internal.outboundservices.IPersonalDataProtector;
import trazzaplatform.api.iam.domain.model.aggregates.IdentityVerification;
import trazzaplatform.api.iam.domain.model.aggregates.UserAccount;
import trazzaplatform.api.iam.domain.repositories.ICarrierProfileRepository;
import trazzaplatform.api.iam.domain.repositories.IIdentityVerificationRepository;
import trazzaplatform.api.iam.domain.repositories.IMerchantProfileRepository;
import trazzaplatform.api.iam.domain.repositories.IPersonIdentityRepository;
import trazzaplatform.api.iam.domain.repositories.IUserRepository;
import trazzaplatform.api.iam.domain.services.IUserQueryService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    public CompletionStage<UserAccount> getAccountAsync(Long userId) {
        return userRepository.findWithRolesAsync(userId)
                .thenCompose(user -> {
                    if (user == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    // Encadenamos las búsquedas del perfil e identidad de forma asíncrona
                    var identityFuture = identityRepository.findByUserIdAsync(userId);
                    var carrierFuture = carrierProfileRepository.findByUserIdAsync(userId);
                    var merchantFuture = merchantProfileRepository.findByUserIdAsync(userId);

                    return CompletableFuture.allOf(identityFuture, carrierFuture, merchantFuture)
                            .thenApply(v -> {
                                var identity = identityFuture.join();
                                var carrier = carrierFuture.join();
                                var merchant = merchantFuture.join();

                                String phone = (identity == null) ? null : dataProtector.decrypt(identity.getPhoneEncrypted());

                                return new UserAccount(user, identity, phone, carrier, merchant);
                            });
                });
    }

    @Override
    public CompletionStage<List<IdentityVerification>> listVerificationsAsync(Long userId) {
        return verificationRepository.listByUserAsync(userId);
    }
}