package com.trazza.platform.billing.domain.model.commands;

import com.trazza.platform.billing.domain.model.valueobjects.CompensationAction;

import java.util.UUID;

public record UpdateCompensationCommand(Long userId, UUID tripId, CompensationAction action) {
}
