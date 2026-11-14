package com.trazza.platform.iam.domain.model.commands;

public record CreateVehicleCommand(Long userId, VehicleData data) {
}
