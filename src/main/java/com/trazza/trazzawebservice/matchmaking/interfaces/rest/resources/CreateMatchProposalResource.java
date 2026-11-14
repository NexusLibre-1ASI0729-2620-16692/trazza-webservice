package com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMatchProposalResource(UUID shipmentRequestId, UUID returnRouteId, BigDecimal offeredRate,
                                          String currency) {
}
