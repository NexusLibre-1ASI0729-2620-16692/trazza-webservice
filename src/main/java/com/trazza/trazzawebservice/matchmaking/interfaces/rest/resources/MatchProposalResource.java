package com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MatchProposalResource(UUID id, BigDecimal offeredRate, String currency, String status,
                                    Instant respondedAt, Instant createdAt, ReturnRouteResource route,
                                    ShipmentRequestResource shipment, PartyResource carrier, PartyResource merchant,
                                    UUID tripId) {
}
