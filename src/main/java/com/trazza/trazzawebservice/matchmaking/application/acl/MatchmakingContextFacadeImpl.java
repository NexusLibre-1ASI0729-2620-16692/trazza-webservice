package com.trazza.trazzawebservice.matchmaking.application.acl;

import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.MatchProposal;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ReturnRoute;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ShipmentRequest;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.MatchProposalStatus;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.ReturnRouteStatus;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.ShipmentRequestStatus;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.MatchProposalRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.MessageRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.ReturnRouteRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.ShipmentRequestRepository;
import com.trazza.trazzawebservice.matchmaking.interfaces.acl.MatchmakingContextFacade;
import com.trazza.trazzawebservice.matchmaking.interfaces.acl.TripContextSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class MatchmakingContextFacadeImpl implements MatchmakingContextFacade {

    private final ReturnRouteRepository returnRouteRepository;
    private final ShipmentRequestRepository shipmentRequestRepository;
    private final MatchProposalRepository matchProposalRepository;
    private final MessageRepository messageRepository;

    public MatchmakingContextFacadeImpl(ReturnRouteRepository returnRouteRepository,
                                        ShipmentRequestRepository shipmentRequestRepository,
                                        MatchProposalRepository matchProposalRepository,
                                        MessageRepository messageRepository) {
        this.returnRouteRepository = returnRouteRepository;
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.matchProposalRepository = matchProposalRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripContextSnapshot> listTripContexts(Collection<Long> matchProposalIds) {
        var snapshots = new ArrayList<TripContextSnapshot>();
        for (var proposalId : new LinkedHashSet<>(matchProposalIds)) {
            var proposal = matchProposalRepository.findById(proposalId).orElse(null);
            if (proposal == null) continue;
            var route = returnRouteRepository.findById(proposal.getReturnRouteId()).orElse(null);
            var shipment = shipmentRequestRepository.findById(proposal.getShipmentRequestId()).orElse(null);
            if (route == null || shipment == null) continue;
            snapshots.add(new TripContextSnapshot(proposal.getId(), proposal.getPublicId(), route.getId(),
                    route.getPublicId(), route.getCarrierProfileId(), route.getVehicleId(), route.getDriverId(),
                    route.getOriginAddress(), route.getOrigin(), route.getDestinationAddress(), route.getDestination(),
                    route.getDepartureAt(), shipment.getId(), shipment.getPublicId(), shipment.getMerchantProfileId(),
                    shipment.getDescription(), shipment.getWeightKg(), shipment.getVolumeM3(),
                    shipment.getPickupAddress(), shipment.getPickup(), shipment.getDeliveryAddress(),
                    shipment.getDelivery()));
        }
        return snapshots;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> listProposalIdsWithTrips(Long carrierProfileId, Long merchantProfileId) {
        var ids = new LinkedHashSet<Long>();
        if (carrierProfileId != null) {
            var routeIds = returnRouteRepository.findByCarrierProfileIdOrderByDepartureAtDesc(carrierProfileId).stream()
                    .map(ReturnRoute::getId).toList();
            matchProposalRepository.findByReturnRouteIdInOrderByCreatedAtDesc(routeIds).stream()
                    .filter(MatchmakingContextFacadeImpl::hasTrip).forEach(p -> ids.add(p.getId()));
        }
        if (merchantProfileId != null) {
            var shipmentIds = shipmentRequestRepository.findByMerchantProfileIdOrderByCreatedAtDesc(merchantProfileId)
                    .stream().map(ShipmentRequest::getId).toList();
            matchProposalRepository.findByShipmentRequestIdInOrderByCreatedAtDesc(shipmentIds).stream()
                    .filter(MatchmakingContextFacadeImpl::hasTrip).forEach(p -> ids.add(p.getId()));
        }
        return List.copyOf(ids);
    }

    @Override
    @Transactional
    public void markPickedUp(Long matchProposalId) {
        matchProposalRepository.findById(matchProposalId).ifPresent(proposal -> {
            shipmentRequestRepository.findById(proposal.getShipmentRequestId()).ifPresent(ShipmentRequest::markInTransit);
            returnRouteRepository.findById(proposal.getReturnRouteId()).ifPresent(ReturnRoute::start);
        });
    }

    @Override
    @Transactional
    public void markDelivered(Long matchProposalId) {
        matchProposalRepository.findById(matchProposalId).ifPresent(proposal -> {
            shipmentRequestRepository.findById(proposal.getShipmentRequestId()).ifPresent(ShipmentRequest::markDelivered);
            completeRouteIfDone(proposal.getReturnRouteId());
        });
    }

    @Override
    @Transactional
    public void release(Long matchProposalId, boolean cancelledByMerchant, String reason, Instant now) {
        var proposal = matchProposalRepository.findById(matchProposalId).orElse(null);
        if (proposal == null) return;
        proposal.withdraw(now);

        shipmentRequestRepository.findById(proposal.getShipmentRequestId()).ifPresent(shipment -> {
            if (cancelledByMerchant || !shipment.getPickupUntil().isAfter(now)) shipment.cancel(reason, now);
            else shipment.reopen();
        });

        var route = returnRouteRepository.findById(proposal.getReturnRouteId()).orElse(null);
        if (route == null) return;
        if (route.getStatus() == ReturnRouteStatus.FULL && route.getDepartureAt().isAfter(now)) route.reopen();
        else completeRouteIfDone(route.getId());
    }

    @Override
    @Transactional
    public int purgeChat(Long matchProposalId) {
        return messageRepository.deleteByProposal(matchProposalId);
    }

    /** A started route is completed once none of its shipments is still waiting or on the way. */
    private void completeRouteIfDone(Long returnRouteId) {
        var route = returnRouteRepository.findById(returnRouteId).orElse(null);
        if (route == null || route.getStatus() != ReturnRouteStatus.IN_PROGRESS) return;
        var accepted = matchProposalRepository.findByReturnRouteIdInOrderByCreatedAtDesc(List.of(route.getId())).stream()
                .filter(p -> p.getStatus() == MatchProposalStatus.ACCEPTED)
                .map(MatchProposal::getShipmentRequestId)
                .toList();
        var done = shipmentRequestRepository.findAllById(accepted).stream()
                .allMatch(s -> s.getStatus() == ShipmentRequestStatus.DELIVERED
                        || s.getStatus() == ShipmentRequestStatus.CANCELLED);
        if (done) route.complete();
    }

    private static boolean hasTrip(MatchProposal proposal) {
        return proposal.getStatus() == MatchProposalStatus.ACCEPTED || proposal.getStatus() == MatchProposalStatus.CANCELLED;
    }
}
