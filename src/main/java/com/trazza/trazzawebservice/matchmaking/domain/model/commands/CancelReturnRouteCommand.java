package com.trazza.trazzawebservice.matchmaking.domain.model.commands;

import java.util.UUID;

public record CancelReturnRouteCommand(Long userId, UUID returnRouteId) {
}
