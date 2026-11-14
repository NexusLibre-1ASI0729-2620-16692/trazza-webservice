package com.trazza.trazzawebservice.matchmaking.domain.services;

import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ShipmentRequest;

public record ShipmentRequestView(ShipmentRequest shipment, String merchantName, int pendingProposals) {
}
