package com.trazza.trazzawebservice.matchmaking.domain.services;

import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.MatchProposal;

import java.util.UUID;

/** A proposal with both sides. {@code tripPublicId} is set once the proposal was accepted. */
public record MatchProposalView(MatchProposal proposal, ReturnRouteView route, ShipmentRequestView shipment,
                                Long carrierUserId, String carrierName, String carrierPhone, Long merchantUserId,
                                String merchantName, String merchantPhone, UUID tripPublicId) {
}
