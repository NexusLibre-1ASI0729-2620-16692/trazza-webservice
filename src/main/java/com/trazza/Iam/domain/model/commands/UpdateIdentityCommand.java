package com.trazza.platform.iam.domain.model.commands;

public record UpdateIdentityCommand(Long userId, String fullName, String phone) {
}
