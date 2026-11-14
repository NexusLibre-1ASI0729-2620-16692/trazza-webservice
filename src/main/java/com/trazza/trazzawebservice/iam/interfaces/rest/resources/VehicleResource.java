package com.trazza.trazzawebservice.iam.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleResource(UUID id, String plate, String model, BigDecimal maxLoadKg, BigDecimal cargoLengthM,
                              BigDecimal cargoWidthM, BigDecimal cargoHeightM, BigDecimal cargoVolumeM3,
                              String photoS3Key, boolean isActive) {
}
