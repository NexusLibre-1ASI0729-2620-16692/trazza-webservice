package com.trazza.iam.application.internal.commandservices;

import com.trazza.iam.domain.model.aggregates.Vehicle;
import com.trazza.iam.domain.model.aggregates.Driver;
import com.trazza.iam.domain.model.aggregates.SavedAddress;
import com.trazza.iam.domain.model.commands.*;
import com.trazza.iam.domain.model.valueobjects.*;
import com.trazza.iam.domain.repositories.*;
import com.trazza.iam.domain.services.IFleetCommandService;
import com.trazza.shared.domain.model.Result;
import com.trazza.shared.domain.model.CommandError;
import com.trazza.shared.domain.repositories.IUnitOfWork;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Fleet of a carrier (vehicles and drivers) and saved addresses of any user.
 */
public class FleetCommandService implements IFleetCommandService {

    public static final BigDecimal MAX_LOAD_KG = new BigDecimal("30000");
    public static final BigDecimal MAX_DIMENSION_M = new BigDecimal("20");

    private final ICarrierProfileRepository carrierProfileRepository;
    private final IVehicleRepository vehicleRepository;
    private final IDriverRepository driverRepository;
    private final ISavedAddressRepository savedAddressRepository;
    private final IUnitOfWork unitOfWork;
    private final Clock timeProvider;

    public FleetCommandService(
            ICarrierProfileRepository carrierProfileRepository,
            IVehicleRepository vehicleRepository,
            IDriverRepository driverRepository,
            ISavedAddressRepository savedAddressRepository,
            IUnitOfWork unitOfWork,
            Clock timeProvider) {
        this.carrierProfileRepository = carrierProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.savedAddressRepository = savedAddressRepository;
        this.unitOfWork = unitOfWork;
        this.timeProvider = timeProvider;
    }

    @Override
    public Result<Vehicle, CommandError> handle(CreateVehicleCommand command) {
        var profile = carrierProfileRepository.findByUserId(command.getUserId());
        if (profile == null) return Result.failure(CommandError.forbidden("carrier-role-required"));

        var data = validateVehicle(command.getData());
        if (data.isFailure()) return Result.failure(data.getError());
        if (vehicleRepository.existsByPlate(data.getValue().getPlate(), null))
            return Result.failure(CommandError.conflict("plate-already-registered"));

        var value = data.getValue();
        var vehicle = new Vehicle(profile.getId(), value.getPlate(), value.getModel(), value.getMaxLoadKg(),
                value.getCargoLengthM(), value.getCargoWidthM(), value.getCargoHeightM(), value.getCargoVolumeM3(),
                value.getPhotoS3Key());
        vehicleRepository.add(vehicle);
        unitOfWork.complete();
        return Result.success(vehicle);
    }

    @Override
    public Result<Vehicle, CommandError> handle(UpdateVehicleCommand command) {
        var owned = findOwnedVehicle(command.getUserId(), command.getVehicleId());
        if (owned.isFailure()) return owned;

        var data = validateVehicle(command.getData());
        if (data.isFailure()) return Result.failure(data.getError());

        var vehicle = owned.getValue();
        if (vehicleRepository.existsByPlate(data.getValue().getPlate(), vehicle.getId()))
            return Result.failure(CommandError.conflict("plate-already-registered"));

        var value = data.getValue();
        vehicle.update(value.getPlate(), value.getModel(), value.getMaxLoadKg(), value.getCargoLengthM(),
                value.getCargoWidthM(), value.getCargoHeightM(), value.getCargoVolumeM3(),
                value.getPhotoS3Key(), value.isActive());
        unitOfWork.complete();
        return Result.success(vehicle);
    }

    @Override
    public Result<Vehicle, CommandError> handle(DeactivateVehicleCommand command) {
        var owned = findOwnedVehicle(command.getUserId(), command.getVehicleId());
        if (owned.isFailure()) return owned;

        owned.getValue().deactivate();
        unitOfWork.complete();
        return owned;
    }

    @Override
    public Result<Driver, CommandError> handle(CreateDriverCommand command) {
        var profile = carrierProfileRepository.findByUserId(command.getUserId());
        if (profile == null) return Result.failure(CommandError.forbidden("carrier-role-required"));

        var data = validateDriver(command.getData());
        if (data.isFailure()) return Result.failure(data.getError());

        var driver = new Driver(profile.getId(), data.getValue().getFullName(), data.getValue().getSex(), (byte) data.getValue().getExperienceStars());
        driverRepository.add(driver);
        unitOfWork.complete();
        return Result.success(driver);
    }

    @Override
    public Result<Driver, CommandError> handle(UpdateDriverCommand command) {
        var owned = findOwnedDriver(command.getUserId(), command.getDriverId());
        if (owned.isFailure()) return owned;

        var data = validateDriver(command.getData());
        if (data.isFailure()) return Result.failure(data.getError());

        owned.getValue().update(data.getValue().getFullName(), data.getValue().getSex(),
                (byte) data.getValue().getExperienceStars(), data.getValue().isActive());
        unitOfWork.complete();
        return owned;
    }

    @Override
    public Result<Driver, CommandError> handle(DeactivateDriverCommand command) {
        var owned = findOwnedDriver(command.getUserId(), command.getDriverId());
        if (owned.isFailure()) return owned;

        owned.getValue().deactivate();
        unitOfWork.complete();
        return owned;
    }

    @Override
    public Result<SavedAddress, CommandError> handle(CreateSavedAddressCommand command) {
        var label = (command.getLabel() == null ? "" : command.getLabel()).trim();
        if (label.length() < 2 || label.length() > 60) return Result.failure(CommandError.validation("address-label-required"));

        var address = (command.getAddress() == null ? "" : command.getAddress()).trim();
        if (address.length() < 5 || address.length() > 255) return Result.failure(CommandError.validation("address-street-required"));

        var district = (command.getDistrict() == null || command.getDistrict().isBlank()) ? null : command.getDistrict().trim();
        if (district != null && district.length() > 80) return Result.failure(CommandError.validation("address-district-invalid"));

        var location = GeoLocation.create(command.getLatitude(), command.getLongitude());
        if (location.isFailure()) return Result.failure(location.getError());

        var saved = savedAddressRepository.add(new SavedAddress(0L, command.getUserId(), label, address, district,
                location.getValue().getLatitude(), location.getValue().getLongitude(), LocalDateTime.ofInstant(timeProvider.instant(), ZoneOffset.UTC)));
        return Result.success(saved);
    }

    @Override
    public Result<Boolean, CommandError> handle(DeleteSavedAddressCommand command) {
        boolean deleted = savedAddressRepository.delete(command.getSavedAddressId(), command.getUserId());
        if (!deleted) return Result.failure(CommandError.notFound("saved-address-not-found"));
        return Result.success(true);
    }

    private Result<Vehicle, CommandError> findOwnedVehicle(Long userId, UUID vehicleId) {
        var profile = carrierProfileRepository.findByUserId(userId);
        if (profile == null) return Result.failure(CommandError.forbidden("carrier-role-required"));

        var vehicle = vehicleRepository.findByPublicId(vehicleId);
        if (vehicle == null || !vehicle.getCarrierProfileId().equals(profile.getId()))
            return Result.failure(CommandError.notFound("vehicle-not-found"));
        return Result.success(vehicle);
    }

    private Result<Driver, CommandError> findOwnedDriver(Long userId, Long driverId) {
        var profile = carrierProfileRepository.findByUserId(userId);
        if (profile == null) return Result.failure(CommandError.forbidden("carrier-role-required"));

        var driver = driverRepository.findById(driverId);
        if (driver == null || !driver.getCarrierProfileId().equals(profile.getId()))
            return Result.failure(CommandError.notFound("driver-not-found"));
        return Result.success(driver);
    }

    private static Result<VehicleData, CommandError> validateVehicle(VehicleData data) {
        var plate = LicensePlate.create(data.getPlate());
        if (plate.isFailure()) return Result.failure(plate.getError());

        var model = (data.getModel() == null ? "" : data.getModel()).trim();
        if (model.length() < 2 || model.length() > 100) return Result.failure(CommandError.validation("vehicle-model-required"));

        if (data.getMaxLoadKg().compareTo(BigDecimal.ZERO) <= 0 || data.getMaxLoadKg().compareTo(MAX_LOAD_KG) > 0)
            return Result.failure(CommandError.validation("capacity-weight-invalid"));

        if (data.getCargoLengthM().compareTo(BigDecimal.ZERO) <= 0 || data.getCargoLengthM().compareTo(MAX_DIMENSION_M) > 0 ||
                data.getCargoWidthM().compareTo(BigDecimal.ZERO) <= 0 || data.getCargoWidthM().compareTo(MAX_DIMENSION_M) > 0 ||
                data.getCargoHeightM().compareTo(BigDecimal.ZERO) <= 0 || data.getCargoHeightM().compareTo(MAX_DIMENSION_M) > 0)
            return Result.failure(CommandError.validation("cargo-dimensions-invalid"));

        BigDecimal volume = (data.getCargoVolumeM3() != null && data.getCargoVolumeM3().compareTo(BigDecimal.ZERO) > 0)
                ? data.getCargoVolumeM3()
                : data.getCargoLengthM().multiply(data.getCargoWidthM()).multiply(data.getCargoHeightM()).setScale(2, RoundingMode.HALF_UP);

        if (volume.compareTo(BigDecimal.ZERO) <= 0 || volume.compareTo(new BigDecimal("99999")) > 0)
            return Result.failure(CommandError.validation("capacity-volume-invalid"));

        var photo = (data.getPhotoS3Key() == null || data.getPhotoS3Key().isBlank()) ? null : data.getPhotoS3Key().trim();

        // En Java no hay "with", se usa un constructor o un builder. Asumo que VehicleData tiene un constructor de copia o métodos set.
        // Si tus VehicleData no son inmutables, puedes hacer data.setPlate(plate.getValue().getValue()), etc.
        // Aquí usé una suposición de que tienes un método copy() o setters.

        data.setPlate(plate.getValue().getValue());
        data.setModel(model);
        data.setCargoVolumeM3(volume);
        data.setPhotoS3Key(photo);
        return Result.success(data);
    }

    private static Result<DriverData, CommandError> validateDriver(DriverData data) {
        var fullName = (data.getFullName() == null ? "" : data.getFullName()).trim();
        if (fullName.length() < 3 || fullName.length() > 200) return Result.failure(CommandError.validation("full-name-required"));

        if (data.getExperienceStars() < 1 || data.getExperienceStars() > 5) return Result.failure(CommandError.validation("experience-stars-invalid"));

        data.setFullName(fullName);
        return Result.success(data);
    }
}