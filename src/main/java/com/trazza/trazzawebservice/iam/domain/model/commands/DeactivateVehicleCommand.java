package com.trazza.trazzawebservice.iam.domain.model.commands;

import java.util.UUID;

public record DeactivateVehicleCommand(Long userId, UUID vehicleId) {
}
