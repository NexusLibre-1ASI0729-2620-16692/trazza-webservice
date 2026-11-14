package com.trazza.iam.interfaces.acl;

import com.trazza.iam.application.internal.outboundservices.PersonalDataProtector;
import com.trazza.iam.domain.model.aggregates.*;
import com.trazza.iam.domain.repositories.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class IamContextFacade {

    // Records equivalentes a las tuplas de C#
    public record VehicleSnapshot(
            Long id, UUID publicId, Long carrierProfileId, String plate,
            String model, BigDecimal maxLoadKg, BigDecimal cargoVolumeM3, Boolean isActive
    ) {}

    public record DriverSnapshot(
            Long id, Long carrierProfileId, String fullName, Byte experienceStars, Boolean isActive
    ) {}

    public record ContactSnapshot(
            Long userId, UUID publicId, String fullName, String phone, String businessName
    ) {}

    /**
     * Anti-corruption layer used by the other bounded contexts to read IAM & Profiles data.
     */
    public interface IamContextFacadePort {
        CompletableFuture<Long> findCarrierProfileId(Long userId);
        CompletableFuture<Long> findMerchantProfileId(Long userId);
        CompletableFuture<Long> findUserIdByCarrierProfileId(Long carrierProfileId);
        CompletableFuture<Long> findUserIdByMerchantProfileId(Long merchantProfileId);
        CompletableFuture<VehicleSnapshot> findVehicle(UUID publicId);
        CompletableFuture<List<VehicleSnapshot>> listVehicles(List<Long> ids);
        CompletableFuture<List<VehicleSnapshot>> listVehiclesByPublicIds(List<UUID> publicIds);
        CompletableFuture<List<VehicleSnapshot>> listVehiclesOfCarrier(Long carrierProfileId);
        CompletableFuture<DriverSnapshot> findDriver(Long driverId);
        CompletableFuture<ContactSnapshot> findContact(Long userId);
        CompletableFuture<Long> findUserIdByPublicId(UUID publicId);
    }

    @Service
    public static class IamContextFacadeImpl implements IamContextFacadePort {

        private final IamRepositories.UserRepository userRepository;
        private final IamRepositories.PersonIdentityRepository identityRepository;
        private final IamRepositories.CarrierProfileRepository carrierProfileRepository;
        private final IamRepositories.MerchantProfileRepository merchantProfileRepository;
        private final IamRepositories.VehicleRepository vehicleRepository;
        private final IamRepositories.DriverRepository driverRepository;
        private final PersonalDataProtector dataProtector;

        public IamContextFacadeImpl(
                IamRepositories.UserRepository userRepository,
                IamRepositories.PersonIdentityRepository identityRepository,
                IamRepositories.CarrierProfileRepository carrierProfileRepository,
                IamRepositories.MerchantProfileRepository merchantProfileRepository,
                IamRepositories.VehicleRepository vehicleRepository,
                IamRepositories.DriverRepository driverRepository,
                PersonalDataProtector dataProtector) {
            this.userRepository = userRepository;
            this.identityRepository = identityRepository;
            this.carrierProfileRepository = carrierProfileRepository;
            this.merchantProfileRepository = merchantProfileRepository;
            this.vehicleRepository = vehicleRepository;
            this.driverRepository = driverRepository;
            this.dataProtector = dataProtector;
        }

        @Override
        public CompletableFuture<Long> findCarrierProfileId(Long userId) {
            return carrierProfileRepository.findByUserId(userId)
                    .thenApply(profile -> profile.map(CarrierProfile::getId).orElse(null));
        }

        @Override
        public CompletableFuture<Long> findMerchantProfileId(Long userId) {
            return merchantProfileRepository.findByUserId(userId)
                    .thenApply(profile -> profile.map(MerchantProfile::getId).orElse(null));
        }

        @Override
        public CompletableFuture<Long> findUserIdByCarrierProfileId(Long carrierProfileId) {
            return carrierProfileRepository.findById(carrierProfileId)
                    .thenApply(profile -> profile.map(CarrierProfile::getUserId).orElse(null));
        }

        @Override
        public CompletableFuture<Long> findUserIdByMerchantProfileId(Long merchantProfileId) {
            return merchantProfileRepository.findById(merchantProfileId)
                    .thenApply(profile -> profile.map(MerchantProfile::getUserId).orElse(null));
        }

        @Override
        public CompletableFuture<VehicleSnapshot> findVehicle(UUID publicId) {
            return vehicleRepository.findByPublicId(publicId)
                    .thenApply(vehicleOpt -> vehicleOpt
                            .map(v -> new VehicleSnapshot(v.getId(), v.getPublicId(), v.getCarrierProfileId(),
                                    v.getPlate(), v.getModel(), v.getMaxLoadKg(), v.getCargoVolumeM3(), v.getIsActive()))
                            .orElse(null));
        }

        @Override
        public CompletableFuture<List<VehicleSnapshot>> listVehicles(List<Long> ids) {
            if (ids.isEmpty()) {
                return CompletableFuture.completedFuture(List.of());
            }
            return CompletableFuture.supplyAsync(() ->
                    ids.stream()
                            // Nota: aquí idealmente usarías vehicleRepository.findByIdIn(ids)
                            // pero para respetar el código original, iteramos
                            .map(id -> vehicleRepository.findById(id).join())
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(v -> new VehicleSnapshot(v.getId(), v.getPublicId(), v.getCarrierProfileId(),
                                    v.getPlate(), v.getModel(), v.getMaxLoadKg(), v.getCargoVolumeM3(), v.getIsActive()))
                            .collect(Collectors.toList())
            );
        }

        @Override
        public CompletableFuture<List<VehicleSnapshot>> listVehiclesOfCarrier(Long carrierProfileId) {
            return vehicleRepository.findByCarrierProfileIdOrderByPlateAsc(carrierProfileId)
                    .thenApply(vehicles -> vehicles.stream()
                            .map(v -> new VehicleSnapshot(v.getId(), v.getPublicId(), v.getCarrierProfileId(),
                                    v.getPlate(), v.getModel(), v.getMaxLoadKg(), v.getCargoVolumeM3(), v.getIsActive()))
                            .collect(Collectors.toList()));
        }

        @Override
        public CompletableFuture<List<VehicleSnapshot>> listVehiclesByPublicIds(List<UUID> publicIds) {
            List<CompletableFuture<VehicleSnapshot>> futures = publicIds.stream()
                    .distinct()
                    .map(this::findVehicle)
                    .collect(Collectors.toList());

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.toList()));
        }

        @Override
        public CompletableFuture<DriverSnapshot> findDriver(Long driverId) {
            return driverRepository.findById(driverId)
                    .thenApply(driverOpt -> driverOpt
                            .map(d -> new DriverSnapshot(d.getId(), d.getCarrierProfileId(), d.getFullName(),
                                    d.getExperienceStars(), d.getIsActive()))
                            .orElse(null));
        }

        @Override
        public CompletableFuture<ContactSnapshot> findContact(Long userId) {
            CompletableFuture<Optional<User>> userFuture = userRepository.findById(userId);
            CompletableFuture<Optional<PersonIdentity>> identityFuture = identityRepository.findByUserId(userId);
            CompletableFuture<Optional<MerchantProfile>> merchantFuture = merchantProfileRepository.findByUserId(userId);

            return CompletableFuture.allOf(userFuture, identityFuture, merchantFuture)
                    .thenApply(v -> {
                        Optional<User> user = userFuture.join();
                        if (user.isEmpty()) return null;

                        Optional<PersonIdentity> identity = identityFuture.join();
                        Optional<MerchantProfile> merchant = merchantFuture.join();

                        String fullName = identity.map(PersonIdentity::getFullName).orElse(user.get().getEmail());
                        String phone = identity.map(i -> dataProtector.decrypt(i.getPhoneEncrypted())).orElse(null);
                        String businessName = merchant.map(MerchantProfile::getBusinessName).orElse(null);

                        return new ContactSnapshot(
                                user.get().getId(),
                                user.get().getPublicId(),
                                fullName,
                                phone,
                                businessName
                        );
                    });
        }

        @Override
        public CompletableFuture<Long> findUserIdByPublicId(UUID publicId) {
            return userRepository.findByPublicId(publicId)
                    .thenApply(userOpt -> userOpt.map(User::getId).orElse(null));
        }
    }
}