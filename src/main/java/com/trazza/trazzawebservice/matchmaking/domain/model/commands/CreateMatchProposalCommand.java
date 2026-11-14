package com.trazza.trazzawebservice.matchmaking.domain.model.commands;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMatchProposalCommand(Long userId, UUID shipmentRequestId, UUID returnRouteId,
                                         BigDecimal offeredRate, String currency) {
}
