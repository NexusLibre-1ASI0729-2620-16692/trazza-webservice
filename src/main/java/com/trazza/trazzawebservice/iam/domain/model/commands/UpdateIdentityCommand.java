package com.trazza.trazzawebservice.iam.domain.model.commands;

public record UpdateIdentityCommand(Long userId, String fullName, String phone) {
}
