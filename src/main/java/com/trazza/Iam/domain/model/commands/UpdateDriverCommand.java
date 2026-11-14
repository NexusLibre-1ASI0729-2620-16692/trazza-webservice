package com.trazza.platform.iam.domain.model.commands;

public record UpdateDriverCommand(Long userId, Long driverId, DriverData data) {
}
