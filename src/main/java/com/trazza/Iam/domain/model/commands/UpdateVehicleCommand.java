package com.trazza.platform.iam.domain.model.commands;

import java.util.UUID;

public record UpdateVehicleCommand(Long userId, UUID vehicleId, VehicleData data) {
}
