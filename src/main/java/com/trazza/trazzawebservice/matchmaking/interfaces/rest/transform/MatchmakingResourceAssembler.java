package com.trazza.trazzawebservice.matchmaking.interfaces.rest.transform;

import com.trazza.trazzawebservice.matchmaking.domain.model.commands.PlaceData;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.MatchProposalStatus;
import com.trazza.trazzawebservice.matchmaking.domain.services.MatchProposalView;
import com.trazza.trazzawebservice.matchmaking.domain.services.MessageView;
import com.trazza.trazzawebservice.matchmaking.domain.services.ReturnRouteView;
import com.trazza.trazzawebservice.matchmaking.domain.services.ShipmentRequestView;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.MatchProposalResource;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.MessageResource;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.PartyResource;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.PlaceResource;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.ReturnRouteResource;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.RouteVehicleResource;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.ShipmentRequestResource;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;

public final class MatchmakingResourceAssembler {

    private MatchmakingResourceAssembler() {
    }

    public static PlaceData toPlace(PlaceResource resource) {
        return resource == null ? new PlaceData("", null, null)
                : new PlaceData(resource.address() == null ? "" : resource.address(), resource.latitude(),
                resource.longitude());
    }

    public static ReturnRouteResource toResource(ReturnRouteView view) {
        var route = view.route();
        return new ReturnRouteResource(route.getPublicId(), place(route.getOriginAddress(), route.getOrigin()),
                place(route.getDestinationAddress(), route.getDestination()), route.getDepartureAt(),
                route.getMaxShipments(), route.getStatus().name(),
                new RouteVehicleResource(view.vehiclePublicId(), view.vehiclePlate(), view.vehicleModel()),
                route.getDriverId(), view.driverName(), view.acceptedShipments(), view.availableWeightKg(),
                view.availableVolumeM3(), view.pendingProposals(), route.getCreatedAt());
    }

    public static ShipmentRequestResource toResource(ShipmentRequestView view) {
        var shipment = view.shipment();
        return new ShipmentRequestResource(shipment.getPublicId(), shipment.getDescription(), shipment.getWeightKg(),
                shipment.getVolumeM3(), place(shipment.getPickupAddress(), shipment.getPickup()),
                place(shipment.getDeliveryAddress(), shipment.getDelivery()), shipment.getPickupFrom(),
                shipment.getPickupUntil(), shipment.getStatus().name(), view.merchantName(), view.pendingProposals(),
                shipment.getCancellationReason(), shipment.getCancelledAt(), shipment.getCreatedAt());
    }

    public static MatchProposalResource toResource(MatchProposalView view) {
        var proposal = view.proposal();
        var showPhones = proposal.getStatus() == MatchProposalStatus.ACCEPTED;
        return new MatchProposalResource(proposal.getPublicId(), proposal.getOfferedRate(), proposal.getCurrency(),
                proposal.getStatus().name(), proposal.getRespondedAt(), proposal.getCreatedAt(),
                toResource(view.route()), toResource(view.shipment()),
                new PartyResource(view.carrierName(), showPhones ? view.carrierPhone() : null),
                new PartyResource(view.merchantName(), showPhones ? view.merchantPhone() : null),
                view.tripPublicId());
    }

    public static MessageResource toResource(MessageView view) {
        var message = view.message();
        return new MessageResource(message.getId(), view.senderPublicId(), view.senderName(), view.isMine(),
                message.getContent(), message.getSentAt(), message.getReadAt());
    }

    private static PlaceResource place(String address, GeoLocation location) {
        return new PlaceResource(address, location.latitude(), location.longitude());
    }
}
