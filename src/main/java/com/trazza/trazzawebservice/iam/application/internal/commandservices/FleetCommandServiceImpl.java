package com.trazza.trazzawebservice.iam.application.internal.commandservices;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.Driver;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.SavedAddress;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.Vehicle;
import com.trazza.trazzawebservice.iam.domain.model.commands.CreateDriverCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.CreateSavedAddressCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.CreateVehicleCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.DeactivateDriverCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.DeactivateVehicleCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.DeleteSavedAddressCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.DriverData;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateDriverCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateVehicleCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.VehicleData;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.LicensePlate;
import com.trazza.trazzawebservice.iam.domain.services.FleetCommandService;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.CarrierProfileRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.SavedAddressRepository;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.VehicleRepository;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Fleet of a carrier (vehicles and drivers) and saved addresses of any user. */
@Service
public class FleetCommandServiceImpl implements FleetCommandService {

    public static final BigDecimal MAX_LOAD_KG = new BigDecimal("30000");
    public static final BigDecimal MAX_DIMENSION_M = new BigDecimal("20");
    private static final BigDecimal MAX_VOLUME_M3 = new BigDecimal("99999");

    private final CarrierProfileRepository carrierProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final SavedAddressRepository savedAddressRepository;

    public FleetCommandServiceImpl(CarrierProfileRepository carrierProfileRepository, VehicleRepository vehicleRepository,
                                   DriverRepository driverRepository, SavedAddressRepository savedAddressRepository) {
        this.carrierProfileRepository = carrierProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.savedAddressRepository = savedAddressRepository;
    }

    @Override
    @Transactional
    public Result<Vehicle> handle(CreateVehicleCommand command) {
        var profile = carrierProfileRepository.findByUserId(command.userId());
        if (profile.isEmpty()) return Result.failure(CommandError.forbidden("carrier-role-required"));
        var data = validateVehicle(command.data());
        if (data.isFailure()) return data.asFailure();
        var value = data.value();
        if (vehicleRepository.existsByPlate(value.plate()))
            return Result.failure(CommandError.conflict("plate-already-registered"));

        var vehicle = new Vehicle(profile.get().getId(), value.plate(), value.model(), value.maxLoadKg(),
                value.cargoLengthM(), value.cargoWidthM(), value.cargoHeightM(), value.cargoVolumeM3(),
                value.photoS3Key());
        return Result.success(vehicleRepository.saveAndFlush(vehicle));
    }

    @Override
    @Transactional
    public Result<Vehicle> handle(UpdateVehicleCommand command) {
        var owned = findOwnedVehicle(command.userId(), command.vehicleId());
        if (owned.isFailure()) return owned;
        var data = validateVehicle(command.data());
        if (data.isFailure()) return data.asFailure();
        var vehicle = owned.value();
        var value = data.value();
        if (vehicleRepository.existsByPlateAndIdNot(value.plate(), vehicle.getId()))
            return Result.failure(CommandError.conflict("plate-already-registered"));

        vehicle.update(value.plate(), value.model(), value.maxLoadKg(), value.cargoLengthM(), value.cargoWidthM(),
                value.cargoHeightM(), value.cargoVolumeM3(), value.photoS3Key(), value.active());
        return Result.success(vehicleRepository.saveAndFlush(vehicle));
    }

    @Override
    @Transactional
    public Result<Vehicle> handle(DeactivateVehicleCommand command) {
        var owned = findOwnedVehicle(command.userId(), command.vehicleId());
        if (owned.isFailure()) return owned;
        owned.value().deactivate();
        return Result.success(vehicleRepository.save(owned.value()));
    }

    @Override
    @Transactional
    public Result<Driver> handle(CreateDriverCommand command) {
        var profile = carrierProfileRepository.findByUserId(command.userId());
        if (profile.isEmpty()) return Result.failure(CommandError.forbidden("carrier-role-required"));
        var data = validateDriver(command.data());
        if (data.isFailure()) return data.asFailure();
        var value = data.value();
        return Result.success(driverRepository.saveAndFlush(
                new Driver(profile.get().getId(), value.fullName(), value.sex(), value.experienceStars())));
    }

    @Override
    @Transactional
    public Result<Driver> handle(UpdateDriverCommand command) {
        var owned = findOwnedDriver(command.userId(), command.driverId());
        if (owned.isFailure()) return owned;
        var data = validateDriver(command.data());
        if (data.isFailure()) return data.asFailure();
        var value = data.value();
        owned.value().update(value.fullName(), value.sex(), value.experienceStars(), value.active());
        return Result.success(driverRepository.saveAndFlush(owned.value()));
    }

    @Override
    @Transactional
    public Result<Driver> handle(DeactivateDriverCommand command) {
        var owned = findOwnedDriver(command.userId(), command.driverId());
        if (owned.isFailure()) return owned;
        owned.value().deactivate();
        return Result.success(driverRepository.save(owned.value()));
    }

    @Override
    @Transactional
    public Result<SavedAddress> handle(CreateSavedAddressCommand command) {
        var label = trim(command.label());
        if (label.length() < 2 || label.length() > 60)
            return Result.failure(CommandError.validation("address-label-required"));
        var address = trim(command.address());
        if (address.length() < 5 || address.length() > 255)
            return Result.failure(CommandError.validation("address-street-required"));
        var district = command.district() == null || command.district().isBlank() ? null : command.district().trim();
        if (district != null && district.length() > 80)
            return Result.failure(CommandError.validation("address-district-invalid"));
        var location = GeoLocation.create(command.latitude(), command.longitude());
        if (location.isFailure()) return location.asFailure();

        return Result.success(savedAddressRepository.saveAndFlush(
                new SavedAddress(command.userId(), label, address, district, location.value(), Instant.now())));
    }

    @Override
    @Transactional
    public Result<Boolean> handle(DeleteSavedAddressCommand command) {
        var deleted = savedAddressRepository.deleteByIdAndUserId(command.savedAddressId(), command.userId());
        return deleted > 0 ? Result.success(true) : Result.failure(CommandError.notFound("saved-address-not-found"));
    }

    private Result<Vehicle> findOwnedVehicle(Long userId, UUID vehicleId) {
        var profile = carrierProfileRepository.findByUserId(userId);
        if (profile.isEmpty()) return Result.failure(CommandError.forbidden("carrier-role-required"));
        return vehicleRepository.findByPublicId(vehicleId)
                .filter(vehicle -> Objects.equals(vehicle.getCarrierProfileId(), profile.get().getId()))
                .map(Result::success)
                .orElseGet(() -> Result.failure(CommandError.notFound("vehicle-not-found")));
    }

    private Result<Driver> findOwnedDriver(Long userId, Long driverId) {
        var profile = carrierProfileRepository.findByUserId(userId);
        if (profile.isEmpty()) return Result.failure(CommandError.forbidden("carrier-role-required"));
        return driverRepository.findById(driverId)
                .filter(driver -> Objects.equals(driver.getCarrierProfileId(), profile.get().getId()))
                .map(Result::success)
                .orElseGet(() -> Result.failure(CommandError.notFound("driver-not-found")));
    }

    private static Result<VehicleData> validateVehicle(VehicleData data) {
        var plate = LicensePlate.create(data.plate());
        if (plate.isFailure()) return plate.asFailure();
        var model = trim(data.model());
        if (model.length() < 2 || model.length() > 100)
            return Result.failure(CommandError.validation("vehicle-model-required"));
        if (!inRange(data.maxLoadKg(), MAX_LOAD_KG))
            return Result.failure(CommandError.validation("capacity-weight-invalid"));
        if (!inRange(data.cargoLengthM(), MAX_DIMENSION_M) || !inRange(data.cargoWidthM(), MAX_DIMENSION_M)
                || !inRange(data.cargoHeightM(), MAX_DIMENSION_M))
            return Result.failure(CommandError.validation("cargo-dimensions-invalid"));
        var volume = data.cargoVolumeM3() != null && data.cargoVolumeM3().signum() > 0
                ? data.cargoVolumeM3()
                : data.cargoLengthM().multiply(data.cargoWidthM()).multiply(data.cargoHeightM())
                .setScale(2, RoundingMode.HALF_EVEN);
        if (!inRange(volume, MAX_VOLUME_M3)) return Result.failure(CommandError.validation("capacity-volume-invalid"));
        var photo = data.photoS3Key() == null || data.photoS3Key().isBlank() ? null : data.photoS3Key().trim();
        return Result.success(new VehicleData(plate.value().value(), model, data.maxLoadKg(), data.cargoLengthM(),
                data.cargoWidthM(), data.cargoHeightM(), volume, photo, data.active()));
    }

    private static Result<DriverData> validateDriver(DriverData data) {
        var fullName = trim(data.fullName());
        if (fullName.length() < 3 || fullName.length() > 200)
            return Result.failure(CommandError.validation("full-name-required"));
        if (data.experienceStars() == null || data.experienceStars() < 1 || data.experienceStars() > 5)
            return Result.failure(CommandError.validation("experience-stars-invalid"));
        return Result.success(new DriverData(fullName, data.sex(), data.experienceStars(), data.active()));
    }

    /** True when {@code 0 < value <= max}. */
    private static boolean inRange(BigDecimal value, BigDecimal max) {
        return value != null && value.signum() > 0 && value.compareTo(max) <= 0;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
