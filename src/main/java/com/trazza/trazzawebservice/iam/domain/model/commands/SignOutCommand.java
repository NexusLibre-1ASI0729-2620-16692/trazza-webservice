package com.trazza.trazzawebservice.iam.domain.model.commands;

public record SignOutCommand(Long userId, String refreshToken) {
}
