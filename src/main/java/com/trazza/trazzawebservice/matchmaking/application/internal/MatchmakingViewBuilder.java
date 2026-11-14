package com.trazza.trazzawebservice.matchmaking.application.internal;

import com.trazza.trazzawebservice.execution.interfaces.acl.ExecutionContextFacade;
import com.trazza.trazzawebservice.iam.interfaces.acl.ContactSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.DriverSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.iam.interfaces.acl.VehicleSnapshot;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.MatchProposal;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ReturnRoute;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ShipmentRequest;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.MatchProposalStatus;
import com.trazza.trazzawebservice.matchmaking.domain.services.MatchProposalView;
import com.trazza.trazzawebservice.matchmaking.domain.services.ReturnRouteView;
import com.trazza.trazzawebservice.matchmaking.domain.services.ShipmentRequestView;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.MatchProposalRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.ReturnRouteRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.ShipmentRequestRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Builds the read models of Matchmaking (routes with free capacity, shipments, proposals with both parties). */
@Component
public class MatchmakingViewBuilder {

    private final ReturnRouteRepository returnRouteRepository;
    private final ShipmentRequestRepository shipmentRequestRepository;
    private final MatchProposalRepository matchProposalRepository;
    private final IamContextFacade iamContextFacade;
    private final ExecutionContextFacade executionContextFacade;

    public MatchmakingViewBuilder(ReturnRouteRepository returnRouteRepository,
                                  ShipmentRequestRepository shipmentRequestRepository,
                                  MatchProposalRepository matchProposalRepository, IamContextFacade iamContextFacade,
                                  ExecutionContextFacade executionContextFacade) {
        this.returnRouteRepository = returnRouteRepository;
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.matchProposalRepository = matchProposalRepository;
        this.iamContextFacade = iamContextFacade;
        this.executionContextFacade = executionContextFacade;
    }

    public List<ReturnRouteView> buildRoutes(List<ReturnRoute> routes) {
        if (routes.isEmpty()) return List.of();
        var vehicles = iamContextFacade.listVehicles(routes.stream().map(ReturnRoute::getVehicleId).distinct().toList())
                .stream().collect(Collectors.toMap(VehicleSnapshot::id, Function.identity()));
        var proposals = matchProposalRepository.findByReturnRouteIdInOrderByCreatedAtDesc(
                routes.stream().map(ReturnRoute::getId).toList());
        var accepted = proposals.stream().filter(p -> p.getStatus() == MatchProposalStatus.ACCEPTED).toList();
        var acceptedShipments = shipmentRequestRepository.findAllById(
                        accepted.stream().map(MatchProposal::getShipmentRequestId).toList())
                .stream().collect(Collectors.toMap(ShipmentRequest::getId, Function.identity()));

        var views = new ArrayList<ReturnRouteView>();
        for (var route : routes) {
            var vehicle = vehicles.get(route.getVehicleId());
            var driverName = route.getDriverId() == null ? null
                    : iamContextFacade.findDriver(route.getDriverId()).map(DriverSnapshot::fullName).orElse(null);
            var carried = accepted.stream()
                    .filter(p -> Objects.equals(p.getReturnRouteId(), route.getId()))
                    .map(p -> acceptedShipments.get(p.getShipmentRequestId()))
                    .filter(Objects::nonNull)
                    .toList();
            var maxLoad = vehicle == null ? BigDecimal.ZERO : vehicle.maxLoadKg();
            var maxVolume = vehicle == null ? BigDecimal.ZERO : vehicle.cargoVolumeM3();
            var usedWeight = carried.stream().map(ShipmentRequest::getWeightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
            var usedVolume = carried.stream().map(ShipmentRequest::getVolumeM3).reduce(BigDecimal.ZERO, BigDecimal::add);
            var pending = (int) proposals.stream()
                    .filter(p -> Objects.equals(p.getReturnRouteId(), route.getId()) && p.isPending()).count();
            views.add(new ReturnRouteView(route,
                    vehicle == null ? null : vehicle.publicId(),
                    vehicle == null ? "" : vehicle.plate(),
                    vehicle == null ? "" : vehicle.model(),
                    driverName,
                    carried.size(),
                    maxLoad.subtract(usedWeight).max(BigDecimal.ZERO),
                    maxVolume.subtract(usedVolume).max(BigDecimal.ZERO),
                    pending));
        }
        return views;
    }

    public ReturnRouteView buildRoute(ReturnRoute route) {
        return buildRoutes(List.of(route)).getFirst();
    }

    public List<ShipmentRequestView> buildShipments(List<ShipmentRequest> shipments) {
        if (shipments.isEmpty()) return List.of();
        var proposals = matchProposalRepository.findByShipmentRequestIdInOrderByCreatedAtDesc(
                shipments.stream().map(ShipmentRequest::getId).toList());
        var names = new HashMap<Long, String>();
        for (var merchantProfileId : shipments.stream().map(ShipmentRequest::getMerchantProfileId).distinct().toList())
            names.put(merchantProfileId, findContactByMerchantProfile(merchantProfileId)
                    .map(ContactSnapshot::displayName).orElse(""));

        return shipments.stream().map(shipment -> new ShipmentRequestView(shipment,
                names.get(shipment.getMerchantProfileId()),
                (int) proposals.stream()
                        .filter(p -> Objects.equals(p.getShipmentRequestId(), shipment.getId()) && p.isPending())
                        .count())).toList();
    }

    public ShipmentRequestView buildShipment(ShipmentRequest shipment) {
        return buildShipments(List.of(shipment)).getFirst();
    }

    public List<MatchProposalView> buildProposals(List<MatchProposal> proposals) {
        if (proposals.isEmpty()) return List.of();
        var routes = buildRoutes(returnRouteRepository.findAllById(
                proposals.stream().map(MatchProposal::getReturnRouteId).distinct().toList()));
        var shipments = buildShipments(shipmentRequestRepository.findAllById(
                proposals.stream().map(MatchProposal::getShipmentRequestId).distinct().toList()));
        Map<Long, java.util.UUID> trips = executionContextFacade.findTripPublicIdsByProposalIds(
                proposals.stream().map(MatchProposal::getId).toList());

        var views = new ArrayList<MatchProposalView>();
        for (var proposal : proposals) {
            var route = routes.stream()
                    .filter(view -> Objects.equals(view.route().getId(), proposal.getReturnRouteId())).findFirst()
                    .orElseThrow();
            var shipment = shipments.stream()
                    .filter(view -> Objects.equals(view.shipment().getId(), proposal.getShipmentRequestId()))
                    .findFirst().orElseThrow();
            var carrier = findContactByCarrierProfile(route.route().getCarrierProfileId()).orElse(null);
            var merchant = findContactByMerchantProfile(shipment.shipment().getMerchantProfileId()).orElse(null);
            views.add(new MatchProposalView(proposal, route, shipment,
                    carrier == null ? 0L : carrier.userId(),
                    carrier == null ? "" : carrier.fullName(),
                    carrier == null ? null : carrier.phone(),
                    merchant == null ? 0L : merchant.userId(),
                    merchant == null ? "" : merchant.displayName(),
                    merchant == null ? null : merchant.phone(),
                    trips.get(proposal.getId())));
        }
        return views;
    }

    public MatchProposalView buildProposal(MatchProposal proposal) {
        return buildProposals(List.of(proposal)).getFirst();
    }

    /** User ids of the carrier and the merchant of a proposal (0 when unknown). */
    public Participants findParticipants(MatchProposal proposal) {
        var carrierUserId = returnRouteRepository.findById(proposal.getReturnRouteId())
                .flatMap(route -> iamContextFacade.findUserIdByCarrierProfileId(route.getCarrierProfileId()))
                .orElse(0L);
        var merchantUserId = shipmentRequestRepository.findById(proposal.getShipmentRequestId())
                .flatMap(shipment -> iamContextFacade.findUserIdByMerchantProfileId(shipment.getMerchantProfileId()))
                .orElse(0L);
        return new Participants(carrierUserId, merchantUserId);
    }

    private java.util.Optional<ContactSnapshot> findContactByMerchantProfile(Long merchantProfileId) {
        return iamContextFacade.findUserIdByMerchantProfileId(merchantProfileId).flatMap(iamContextFacade::findContact);
    }

    private java.util.Optional<ContactSnapshot> findContactByCarrierProfile(Long carrierProfileId) {
        return iamContextFacade.findUserIdByCarrierProfileId(carrierProfileId).flatMap(iamContextFacade::findContact);
    }

    public record Participants(Long carrierUserId, Long merchantUserId) {

        public boolean includes(Long userId) {
            return Objects.equals(userId, carrierUserId) || Objects.equals(userId, merchantUserId);
        }
    }
}
