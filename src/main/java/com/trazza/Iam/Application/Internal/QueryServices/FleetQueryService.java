package trazzaplatform.api.iam.application.internal.queryservices;

import trazzaplatform.api.iam.domain.model.aggregates.Driver;
import trazzaplatform.api.iam.domain.model.aggregates.SavedAddress;
import trazzaplatform.api.iam.domain.model.aggregates.Vehicle;
import trazzaplatform.api.iam.domain.repositories.ICarrierProfileRepository;
import trazzaplatform.api.iam.domain.repositories.IDriverRepository;
import trazzaplatform.api.iam.domain.repositories.ISavedAddressRepository;
import trazzaplatform.api.iam.domain.repositories.IVehicleRepository;
import trazzaplatform.api.iam.domain.services.IFleetQueryService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    public CompletionStage<List<Vehicle>> listVehiclesAsync(Long userId) {
        return carrierProfileRepository.findByUserIdAsync(userId)
                .thenCompose(profile -> {
                    if (profile == null) {
                        return CompletableFuture.completedFuture(Collections.emptyList());
                    }
                    return vehicleRepository.listByCarrierAsync(profile.getId());
                });
    }

    @Override
    public CompletionStage<List<Driver>> listDriversAsync(Long userId) {
        return carrierProfileRepository.findByUserIdAsync(userId)
                .thenCompose(profile -> {
                    if (profile == null) {
                        return CompletableFuture.completedFuture(Collections.emptyList());
                    }
                    return driverRepository.listByCarrierAsync(profile.getId());
                });
    }

    @Override
    public CompletionStage<List<SavedAddress>> listSavedAddressesAsync(Long userId) {
        return savedAddressRepository.listByUserAsync(userId);
    }
}