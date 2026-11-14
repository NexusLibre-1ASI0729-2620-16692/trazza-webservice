package com.trazza.trazzawebservice.iam.interfaces.acl;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleSnapshot(Long id, UUID publicId, Long carrierProfileId, String plate, String model,
                              BigDecimal maxLoadKg, BigDecimal cargoVolumeM3, boolean active) {
}
