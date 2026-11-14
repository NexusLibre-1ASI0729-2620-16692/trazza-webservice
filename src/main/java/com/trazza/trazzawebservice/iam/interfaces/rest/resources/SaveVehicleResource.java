package com.trazza.trazzawebservice.iam.interfaces.rest.resources;

import java.math.BigDecimal;

public record SaveVehicleResource(String plate, String model, BigDecimal maxLoadKg, BigDecimal cargoLengthM,
                                  BigDecimal cargoWidthM, BigDecimal cargoHeightM, BigDecimal cargoVolumeM3,
                                  String photoS3Key, Boolean isActive) {
}
