package com.trazza.platform.billing.domain.model.commands;

import java.util.UUID;

public record MarkFreightPaidCommand(Long userId, UUID tripId, String externalReference) {
}
