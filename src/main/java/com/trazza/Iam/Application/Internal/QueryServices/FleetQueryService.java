package com.trazza.iam.application.internal.queryservices;

import com.trazza.iam.domain.model.aggregates.Driver;
import com.trazza.iam.domain.model.aggregates.SavedAddress;
import com.trazza.iam.domain.model.aggregates.Vehicle;
import com.trazza.iam.domain.repositories.ICarrierProfileRepository;
import com.trazza.iam.domain.repositories.IDriverRepository;
import com.trazza.iam.domain.repositories.ISavedAddressRepository;
import com.trazza.iam.domain.repositories.IVehicleRepository;
import com.trazza.iam.domain.services.IFleetQueryService;

import java.util.Collections;
import java.util.List;

public class FleetQueryService implements IFleetQueryService {

    private final ICarrierProfileRepository carrierProfileRepository;
    private final IVehicleRepository vehicleRepository;
    private final IDriverRepository driverRepository;
    private final ISavedAddressRepository savedAddressRepository;

    public FleetQueryService(
            ICarrierProfileRepository carrierProfileRepository,
            IVehicleRepository vehicleRepository,
            IDriverRepository driverRepository,
            ISavedAddressRepository savedAddressRepository) {
        this.carrierProfileRepository = carrierProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.savedAddressRepository = savedAddressRepository;
    }

    @Override
    public List<Vehicle> listVehicles(Long userId) {
        var profile = carrierProfileRepository.findByUserId(userId);
        if (profile == null) {
            return Collections.emptyList();
        }
        return vehicleRepository.listByCarrier(profile.getId());
    }

    @Override
    public List<Driver> listDrivers(Long userId) {
        var profile = carrierProfileRepository.findByUserId(userId);
        if (profile == null) {
            return Collections.emptyList();
        }
        return driverRepository.listByCarrier(profile.getId());
    }

    @Override
    public List<SavedAddress> listSavedAddresses(Long userId) {
        return savedAddressRepository.listByUser(userId);
    }
}