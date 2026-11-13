package com.trazza.platform.billing.domain.model.commands;

import java.util.UUID;

public record ConfirmFreightReceivedCommand(Long userId, UUID tripId) {
}
