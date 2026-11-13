package com.trazza.platform.billing.domain.model.commands;

import java.util.List;
import java.util.UUID;

public record SubscribeCommand(Long userId, String planCode, List<UUID> vehicleIds) {
}
