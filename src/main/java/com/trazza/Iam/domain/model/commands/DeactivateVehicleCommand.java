package com.trazza.platform.iam.domain.model.commands;

import java.util.UUID;

public record DeactivateVehicleCommand(Long userId, UUID vehicleId) {
}
