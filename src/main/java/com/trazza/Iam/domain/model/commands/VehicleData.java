package com.trazza.platform.iam.domain.model.commands;

import java.math.BigDecimal;

public record VehicleData(String plate, String model, BigDecimal maxLoadKg, BigDecimal cargoLengthM,
                          BigDecimal cargoWidthM, BigDecimal cargoHeightM, BigDecimal cargoVolumeM3, String photoS3Key,
                          boolean active) {
}
