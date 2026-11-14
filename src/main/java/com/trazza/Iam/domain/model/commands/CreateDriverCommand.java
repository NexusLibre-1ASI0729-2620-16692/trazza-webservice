package com.trazza.platform.iam.domain.model.commands;

public record CreateDriverCommand(Long userId, DriverData data) {
}
