package com.trazza.trazzawebservice.iam.domain.model.commands;

public record CreateVehicleCommand(Long userId, VehicleData data) {
}
