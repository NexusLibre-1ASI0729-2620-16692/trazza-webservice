package com.trazza.trazzawebservice.iam.interfaces.acl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Anti-corruption layer used by the other bounded contexts to read IAM &amp; Profiles data.
 */
public interface IamContextFacade {

    Optional<Long> findCarrierProfileId(Long userId);

    Optional<Long> findMerchantProfileId(Long userId);

    Optional<Long> findUserIdByCarrierProfileId(Long carrierProfileId);

    Optional<Long> findUserIdByMerchantProfileId(Long merchantProfileId);

    Optional<VehicleSnapshot> findVehicle(UUID publicId);

    List<VehicleSnapshot> listVehicles(Collection<Long> ids);

    List<VehicleSnapshot> listVehiclesByPublicIds(Collection<UUID> publicIds);

    List<VehicleSnapshot> listVehiclesOfCarrier(Long carrierProfileId);

    Optional<DriverSnapshot> findDriver(Long driverId);

    Optional<ContactSnapshot> findContact(Long userId);

    Optional<Long> findUserIdByPublicId(UUID publicId);
}
