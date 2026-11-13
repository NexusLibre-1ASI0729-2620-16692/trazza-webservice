package com.trazza.trazzawebservice.execution.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record IotDeviceResource(Long id, String serialNumber, String status, UUID vehicleId, String vehiclePlate,
                                Instant loanedAt) {
}
