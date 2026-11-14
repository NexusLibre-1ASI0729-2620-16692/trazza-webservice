package com.trazza.trazzawebservice.iam.application.acl;

import com.trazza.trazzawebservice.iam.application.internal.outboundservices.security.PersonalDataProtector;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.CarrierProfile;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.MerchantProfile;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.User;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.Vehicle;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.CarrierProfileRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.MerchantProfileRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.PersonIdentityRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.VehicleRepository;
import com.trazza.trazzawebservice.iam.interfaces.acl.ContactSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.DriverSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.iam.interfaces.acl.VehicleSnapshot;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IamContextFacadeImpl implements IamContextFacade {

    private final UserRepository userRepository;
    private final PersonIdentityRepository identityRepository;
    private final CarrierProfileRepository carrierProfileRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final PersonalDataProtector dataProtector;

    public IamContextFacadeImpl(UserRepository userRepository, PersonIdentityRepository identityRepository,
                                CarrierProfileRepository carrierProfileRepository,
                                MerchantProfileRepository merchantProfileRepository, VehicleRepository vehicleRepository,
                                DriverRepository driverRepository, PersonalDataProtector dataProtector) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.carrierProfileRepository = carrierProfileRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.dataProtector = dataProtector;
    }

    @Override
    public Optional<Long> findCarrierProfileId(Long userId) {
        return carrierProfileRepository.findByUserId(userId).map(CarrierProfile::getId);
    }

    @Override
    public Optional<Long> findMerchantProfileId(Long userId) {
        return merchantProfileRepository.findByUserId(userId).map(MerchantProfile::getId);
    }

    @Override
    public Optional<Long> findUserIdByCarrierProfileId(Long carrierProfileId) {
        return carrierProfileRepository.findById(carrierProfileId).map(CarrierProfile::getUserId);
    }

    @Override
    public Optional<Long> findUserIdByMerchantProfileId(Long merchantProfileId) {
        return merchantProfileRepository.findById(merchantProfileId).map(MerchantProfile::getUserId);
    }

    @Override
    public Optional<VehicleSnapshot> findVehicle(UUID publicId) {
        return vehicleRepository.findByPublicId(publicId).map(IamContextFacadeImpl::toSnapshot);
    }

    @Override
    public List<VehicleSnapshot> listVehicles(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return vehicleRepository.findAllById(ids).stream().map(IamContextFacadeImpl::toSnapshot).toList();
    }

    @Override
    public List<VehicleSnapshot> listVehiclesByPublicIds(Collection<UUID> publicIds) {
        return publicIds.stream().distinct().map(this::findVehicle).flatMap(Optional::stream).toList();
    }

    @Override
    public List<VehicleSnapshot> listVehiclesOfCarrier(Long carrierProfileId) {
        return vehicleRepository.findByCarrierProfileIdOrderByPlate(carrierProfileId).stream()
                .map(IamContextFacadeImpl::toSnapshot).toList();
    }

    @Override
    public Optional<DriverSnapshot> findDriver(Long driverId) {
        return driverRepository.findById(driverId).map(driver -> new DriverSnapshot(driver.getId(),
                driver.getCarrierProfileId(), driver.getFullName(), driver.getExperienceStars(), driver.isActive()));
    }

    @Override
    public Optional<ContactSnapshot> findContact(Long userId) {
        if (userId == null) return Optional.empty();
        return userRepository.findById(userId).map(user -> {
            var identity = identityRepository.findById(userId).orElse(null);
            var merchant = merchantProfileRepository.findByUserId(userId).orElse(null);
            return new ContactSnapshot(user.getId(), user.getPublicId(),
                    identity == null ? user.getEmail() : identity.getFullName(),
                    identity == null ? null : dataProtector.decrypt(identity.getPhoneEncrypted()),
                    merchant == null ? null : merchant.getBusinessName());
        });
    }

    @Override
    public Optional<Long> findUserIdByPublicId(UUID publicId) {
        return userRepository.findByPublicId(publicId).map(User::getId);
    }

    private static VehicleSnapshot toSnapshot(Vehicle vehicle) {
        return new VehicleSnapshot(vehicle.getId(), vehicle.getPublicId(), vehicle.getCarrierProfileId(),
                vehicle.getPlate(), vehicle.getModel(), vehicle.getMaxLoadKg(), vehicle.getCargoVolumeM3(),
                vehicle.isActive());
    }
}
