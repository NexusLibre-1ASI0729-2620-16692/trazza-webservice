package com.trazza.trazzawebservice.iam.application.internal.queryservices;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.Driver;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.SavedAddress;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.Vehicle;
import com.trazza.trazzawebservice.iam.domain.services.FleetQueryService;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.CarrierProfileRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.SavedAddressRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FleetQueryServiceImpl implements FleetQueryService {

    private final CarrierProfileRepository carrierProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final SavedAddressRepository savedAddressRepository;

    public FleetQueryServiceImpl(CarrierProfileRepository carrierProfileRepository, VehicleRepository vehicleRepository,
                                 DriverRepository driverRepository, SavedAddressRepository savedAddressRepository) {
        this.carrierProfileRepository = carrierProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.savedAddressRepository = savedAddressRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> listVehicles(Long userId) {
        return carrierProfileRepository.findByUserId(userId)
                .map(profile -> vehicleRepository.findByCarrierProfileIdOrderByPlate(profile.getId()))
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Driver> listDrivers(Long userId) {
        return carrierProfileRepository.findByUserId(userId)
                .map(profile -> driverRepository.findByCarrierProfileIdOrderByFullName(profile.getId()))
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedAddress> listSavedAddresses(Long userId) {
        return savedAddressRepository.findByUserIdOrderByLabel(userId);
    }
}
