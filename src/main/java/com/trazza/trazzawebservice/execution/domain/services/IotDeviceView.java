package com.trazza.trazzawebservice.execution.domain.services;

import com.trazza.trazzawebservice.execution.domain.model.aggregates.IotDevice;

import java.util.UUID;

public record IotDeviceView(IotDevice device, UUID vehiclePublicId, String vehiclePlate) {
}
