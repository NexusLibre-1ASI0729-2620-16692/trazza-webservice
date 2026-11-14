package com.trazza.trazzawebservice.matchmaking.domain.model.commands;

import java.util.UUID;

public record CancelShipmentRequestCommand(Long userId, UUID shipmentRequestId, String reason) {
}
