package com.trazza.iam.domain.model.commands;

import com.trazza.iam.domain.model.valueobjects.DriverSex;
import java.math.BigDecimal;
import java.util.UUID;

public class FleetCommands {

    public record VehicleData(
            String plate, String model, BigDecimal maxLoadKg,
            BigDecimal cargoLengthM, BigDecimal cargoWidthM, BigDecimal cargoHeightM,
            BigDecimal cargoVolumeM3, String photoS3Key, Boolean isActive
    ) {}

    public record CreateVehicleCommand(Long userId, VehicleData data) {}

    public record UpdateVehicleCommand(Long userId, UUID vehicleId, VehicleData data) {}

    public record DeactivateVehicleCommand(Long userId, UUID vehicleId) {}


    public record DriverData(String fullName, DriverSex sex, Integer experienceStars, Boolean isActive) {}

    public record CreateDriverCommand(Long userId, DriverData data) {}

    public record UpdateDriverCommand(Long userId, Long driverId, DriverData data) {}

    public record DeactivateDriverCommand(Long userId, Long driverId) {}


    public record CreateSavedAddressCommand(
            Long userId, String label, String address, String district, Double latitude, Double longitude
    ) {}

    public record DeleteSavedAddressCommand(Long userId, Long savedAddressId) {}
}