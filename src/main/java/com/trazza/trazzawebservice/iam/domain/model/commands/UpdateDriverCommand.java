package com.trazza.trazzawebservice.iam.domain.model.commands;

public record UpdateDriverCommand(Long userId, Long driverId, DriverData data) {
}
