package com.trazza.trazzawebservice.matchmaking.domain.services;

import com.trazza.trazzawebservice.shared.domain.model.Result;

import java.util.List;
import java.util.UUID;

public interface MatchmakingQueryService {

    List<ReturnRouteView> listMyRoutes(Long userId);

    Result<ReturnRouteView> getRoute(Long userId, UUID routeId);

    /** Open shipments that fit the route: near its path, inside its capacity and time window. */
    Result<List<ShipmentRequestView>> suggestShipments(Long userId, UUID routeId);

    List<ShipmentRequestView> listMyShipments(Long userId);

    /** Open shipments any carrier can browse. */
    List<ShipmentRequestView> listOpenShipments();

    /** Published routes that could carry the shipment. */
    Result<List<ReturnRouteView>> suggestRoutes(Long userId, UUID shipmentId);

    /** Proposals where the user is the carrier or the merchant. */
    List<MatchProposalView> listMyProposals(Long userId);

    Result<MatchProposalView> getProposal(Long userId, UUID proposalId);

    /** Chat of a proposal; marks the other party's messages as read. */
    Result<List<MessageView>> listMessages(Long userId, UUID proposalId);
}
