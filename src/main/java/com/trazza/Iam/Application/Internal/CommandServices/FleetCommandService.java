package trazzaplatform.api.iam.application.internal.commandservices;

import trazzaplatform.api.iam.domain.model.aggregates.CarrierProfile;
import trazzaplatform.api.iam.domain.model.aggregates.Driver;
import trazzaplatform.api.iam.domain.model.aggregates.SavedAddress;
import trazzaplatform.api.iam.domain.model.aggregates.Vehicle;
import trazzaplatform.api.iam.domain.model.commands.CreateDriverCommand;
import trazzaplatform.api.iam.domain.model.commands.CreateSavedAddressCommand;
import trazzaplatform.api.iam.domain.model.commands.CreateVehicleCommand;
import trazzaplatform.api.iam.domain.model.commands.DeactivateDriverCommand;
import trazzaplatform.api.iam.domain.model.commands.DeactivateVehicleCommand;
import trazzaplatform.api.iam.domain.model.commands.DeleteSavedAddressCommand;
import trazzaplatform.api.iam.domain.model.commands.DriverData;
import trazzaplatform.api.iam.domain.model.commands.UpdateDriverCommand;
import trazzaplatform.api.iam.domain.model.commands.UpdateVehicleCommand;
import trazzaplatform.api.iam.domain.model.commands.VehicleData;
import trazzaplatform.api.iam.domain.model.valueobjects.GeoLocation;
import trazzaplatform.api.iam.domain.model.valueobjects.LicensePlate;
import trazzaplatform.api.iam.domain.repositories.ICarrierProfileRepository;
import trazzaplatform.api.iam.domain.repositories.IDriverRepository;
import trazzaplatform.api.iam.domain.repositories.ISavedAddressRepository;
import trazzaplatform.api.iam.domain.repositories.IVehicleRepository;
import trazzaplatform.api.iam.domain.services.IFleetCommandService;
import trazzaplatform.api.shared.domain.model.CommandError;
import trazzaplatform.api.shared.domain.model.Result;
import trazzaplatform.api.shared.domain.repositories.IUnitOfWork;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 *     Fleet of a carrier (vehicles and drivers) and saved addresses of any user.
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

    public CompletionStage<Result<Vehicle, CommandError>> handle(CreateVehicleCommand command) {
        return carrierProfileRepository.findByUserIdAsync(command.getUserId())
                .thenCompose(profile -> {
                    if (profile == null)
                        return CompletableFuture.completedFuture(Result.failure(CommandError.forbidden("carrier-role-required")));

                    var data = validateVehicle(command.getData());
                    if (data.isFailure())
                        return CompletableFuture.completedFuture(Result.failure(data.getError()));

                    return vehicleRepository.existsByPlateAsync(data.getValue().getPlate(), null)
                            .thenCompose(exists -> {
                                if (exists)
                                    return CompletableFuture.completedFuture(
                                            Result.failure(CommandError.conflict("plate-already-registered")));

                                var value = data.getValue();
                                var vehicle = new Vehicle(profile.getId(), value.getPlate(), value.getModel(),
                                        value.getMaxLoadKg(), value.getCargoLengthM(), value.getCargoWidthM(),
                                        value.getCargoHeightM(), value.getCargoVolumeM3(), value.getPhotoS3Key());

                                return vehicleRepository.addAsync(vehicle)
                                        .thenCompose(v -> unitOfWork.completeAsync())
                                        .thenApply(v -> Result.<Vehicle, CommandError>success(vehicle));
                            });
                });
    }

    public CompletionStage<Result<Vehicle, CommandError>> handle(UpdateVehicleCommand command) {
        return findOwnedVehicleAsync(command.getUserId(), command.getVehicleId())
                .thenCompose(owned -> {
                    if (owned.isFailure())
                        return CompletableFuture.completedFuture(Result.<Vehicle, CommandError>failure(owned.getError()));

                    var data = validateVehicle(command.getData());
                    if (data.isFailure())
                        return CompletableFuture.completedFuture(Result.<Vehicle, CommandError>failure(data.getError()));

                    var vehicle = owned.getValue();
                    return vehicleRepository.existsByPlateAsync(data.getValue().getPlate(), vehicle.getId())
                            .thenCompose(exists -> {
                                if (exists)
                                    return CompletableFuture.completedFuture(
                                            Result.<Vehicle, CommandError>failure(
                                                    CommandError.conflict("plate-already-registered")));

                                var value = data.getValue();
                                vehicle.update(value.getPlate(), value.getModel(), value.getMaxLoadKg(),
                                        value.getCargoLengthM(), value.getCargoWidthM(), value.getCargoHeightM(),
                                        value.getCargoVolumeM3(), value.getPhotoS3Key(), value.isActive());

                                return unitOfWork.completeAsync()
                                        .thenApply(v -> Result.<Vehicle, CommandError>success(vehicle));
                            });
                });
    }

    public CompletionStage<Result<Vehicle, CommandError>> handle(DeactivateVehicleCommand command) {
        return findOwnedVehicleAsync(command.getUserId(), command.getVehicleId())
                .thenCompose(owned -> {
                    if (owned.isFailure())
                        return CompletableFuture.completedFuture(Result.<Vehicle, CommandError>failure(owned.getError()));

                    owned.getValue().deactivate();
                    return unitOfWork.completeAsync()
                            .thenApply(v -> Result.<Vehicle, CommandError>success(owned.getValue()));
                });
    }

    public CompletionStage<Result<Driver, CommandError>> handle(CreateDriverCommand command) {
        return carrierProfileRepository.findByUserIdAsync(command.getUserId())
                .thenCompose(profile -> {
                    if (profile == null)
                        return CompletableFuture.completedFuture(Result.<Driver, CommandError>failure(
                                CommandError.forbidden("carrier-role-required")));

                    var data = validateDriver(command.getData());
                    if (data.isFailure())
                        return CompletableFuture.completedFuture(Result.<Driver, CommandError>failure(data.getError()));

                    var driver = new Driver(profile.getId(), data.getValue().getFullName(),
                            data.getValue().getSex(), (byte) data.getValue().getExperienceStars());

                    return driverRepository.addAsync(driver)
                            .thenCompose(d -> unitOfWork.completeAsync())
                            .thenApply(d -> Result.<Driver, CommandError>success(driver));
                });
    }

    public CompletionStage<Result<Driver, CommandError>> handle(UpdateDriverCommand command) {
        return findOwnedDriverAsync(command.getUserId(), command.getDriverId())
                .thenCompose(owned -> {
                    if (owned.isFailure())
                        return CompletableFuture.completedFuture(Result.<Driver, CommandError>failure(owned.getError()));

                    var data = validateDriver(command.getData());
                    if (data.isFailure())
                        return CompletableFuture.completedFuture(Result.<Driver, CommandError>failure(data.getError()));

                    owned.getValue().update(data.getValue().getFullName(), data.getValue().getSex(),
                            (byte) data.getValue().getExperienceStars(), data.getValue().isActive());

                    return unitOfWork.completeAsync()
                            .thenApply(v -> Result.<Driver, CommandError>success(owned.getValue()));
                });
    }

    public CompletionStage<Result<Driver, CommandError>> handle(DeactivateDriverCommand command) {
        return findOwnedDriverAsync(command.getUserId(), command.getDriverId())
                .thenCompose(owned -> {
                    if (owned.isFailure())
                        return CompletableFuture.completedFuture(Result.<Driver, CommandError>failure(owned.getError()));

                    owned.getValue().deactivate();
                    return unitOfWork.completeAsync()
                            .thenApply(v -> Result.<Driver, CommandError>success(owned.getValue()));
                });
    }

    public CompletionStage<Result<SavedAddress, CommandError>> handle(CreateSavedAddressCommand command) {
        var label = (command.getLabel() == null ? "" : command.getLabel()).trim();
        if (label.length() < 2 || label.length() > 60)
            return CompletableFuture.completedFuture(Result.failure(CommandError.validation("address-label-required")));

        var address = (command.getAddress() == null ? "" : command.getAddress()).trim();
        if (address.length() < 5 || address.length() > 255)
            return CompletableFuture.completedFuture(Result.failure(CommandError.validation("address-street-required")));

        var district = (command.getDistrict() == null || command.getDistrict().isBlank())
                ? null : command.getDistrict().trim();
        if (district != null && district.length() > 80)
            return CompletableFuture.completedFuture(Result.failure(CommandError.validation("address-district-invalid")));

        var location = GeoLocation.create(command.getLatitude(), command.getLongitude());
        if (location.isFailure())
            return CompletableFuture.completedFuture(Result.failure(location.getError()));

        var savedAddress = new SavedAddress(0L, command.getUserId(), label, address, district,
                location.getValue().getLatitude(), location.getValue().getLongitude(),
                LocalDateTime.ofInstant(timeProvider.instant(), ZoneOffset.UTC));

        return savedAddressRepository.addAsync(savedAddress)
                .thenApply(saved -> Result.<SavedAddress, CommandError>success(saved));
    }

    public CompletionStage<Result<Boolean, CommandError>> handle(DeleteSavedAddressCommand command) {
        return savedAddressRepository.deleteAsync(command.getSavedAddressId(), command.getUserId())
                .thenApply(deleted -> {
                    if (!deleted)
                        return Result.<Boolean, CommandError>failure(
                                CommandError.notFound("saved-address-not-found"));
                    return Result.<Boolean, CommandError>success(true);
                });
    }

    private CompletionStage<Result<Vehicle, CommandError>> findOwnedVehicleAsync(Long userId, UUID vehicleId) {
        return carrierProfileRepository.findByUserIdAsync(userId)
                .thenCompose(profile -> {
                    if (profile == null)
                        return CompletableFuture.completedFuture(Result.<Vehicle, CommandError>failure(
                                CommandError.forbidden("carrier-role-required")));

                    return vehicleRepository.findByPublicIdAsync(vehicleId)
                            .thenApply(vehicle -> {
                                if (vehicle == null || !vehicle.getCarrierProfileId().equals(profile.getId()))
                                    return Result.<Vehicle, CommandError>failure(
                                            CommandError.notFound("vehicle-not-found"));
                                return Result.<Vehicle, CommandError>success(vehicle);
                            });
                });
    }

    private CompletionStage<Result<Driver, CommandError>> findOwnedDriverAsync(Long userId, Long driverId) {
        return carrierProfileRepository.findByUserIdAsync(userId)
                .thenCompose(profile -> {
                    if (profile == null)
                        return CompletableFuture.completedFuture(Result.<Driver, CommandError>failure(
                                CommandError.forbidden("carrier-role-required")));

                    return driverRepository.findByIdAsync(driverId)
                            .thenApply(driver -> {
                                if (driver == null || !driver.getCarrierProfileId().equals(profile.getId()))
                                    return Result.<Driver, CommandError>failure(
                                            CommandError.notFound("driver-not-found"));
                                return Result.<Driver, CommandError>success(driver);
                            });
                });
    }

    private static Result<VehicleData, CommandError> validateVehicle(VehicleData data) {
        var plate = LicensePlate.create(data.getPlate());
        if (plate.isFailure()) return Result.failure(plate.getError());

        var model = (data.getModel() == null ? "" : data.getModel()).trim();
        if (model.length() < 2 || model.length() > 100)
            return Result.failure(CommandError.validation("vehicle-model-required"));

        if (data.getMaxLoadKg() == null
                || data.getMaxLoadKg().compareTo(BigDecimal.ZERO) <= 0
                || data.getMaxLoadKg().compareTo(MAX_LOAD_KG) > 0)
            return Result.failure(CommandError.validation("capacity-weight-invalid"));

        if (data.getCargoLengthM() == null
                || data.getCargoLengthM().compareTo(BigDecimal.ZERO) <= 0
                || data.getCargoLengthM().compareTo(MAX_DIMENSION_M) > 0
                || data.getCargoWidthM() == null
                || data.getCargoWidthM().compareTo(BigDecimal.ZERO) <= 0
                || data.getCargoWidthM().compareTo(MAX_DIMENSION_M) > 0
                || data.getCargoHeightM() == null
                || data.getCargoHeightM().compareTo(BigDecimal.ZERO) <= 0
                || data.getCargoHeightM().compareTo(MAX_DIMENSION_M) > 0)
            return Result.failure(CommandError.validation("cargo-dimensions-invalid"));

        BigDecimal volume;
        if (data.getCargoVolumeM3() != null && data.getCargoVolumeM3().compareTo(BigDecimal.ZERO) > 0) {
            volume = data.getCargoVolumeM3();
        } else {
            volume = data.getCargoLengthM()
                    .multiply(data.getCargoWidthM())
                    .multiply(data.getCargoHeightM())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        if (volume.compareTo(BigDecimal.ZERO) <= 0 || volume.compareTo(new BigDecimal("99999")) > 0)
            return Result.failure(CommandError.validation("capacity-volume-invalid"));

        var photo = (data.getPhotoS3Key() == null || data.getPhotoS3Key().isBlank())
                ? null : data.getPhotoS3Key().trim();

        return Result.success(data.with(
                plate.getValue().getValue(),
                model,
                volume,
                photo));
    }

    private static Result<DriverData, CommandError> validateDriver(DriverData data) {
        var fullName = (data.getFullName() == null ? "" : data.getFullName()).trim();
        if (fullName.length() < 3 || fullName.length() > 200)
            return Result.failure(CommandError.validation("full-name-required"));
        if (data.getExperienceStars() < 1 || data.getExperienceStars() > 5)
            return Result.failure(CommandError.validation("experience-stars-invalid"));

        return Result.success(data.withFullName(fullName));
    }
}