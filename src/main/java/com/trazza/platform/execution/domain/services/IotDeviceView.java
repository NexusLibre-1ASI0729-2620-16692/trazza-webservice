package com.trazza.platform.execution.domain.services;

import com.trazza.platform.execution.domain.model.aggregates.IotDevice;

import java.util.UUID;

public record IotDeviceView(IotDevice device, UUID vehiclePublicId, String vehiclePlate) {
}
