package com.trazza.platform.iam.domain.model.commands;

public record SignOutCommand(Long userId, String refreshToken) {
}
