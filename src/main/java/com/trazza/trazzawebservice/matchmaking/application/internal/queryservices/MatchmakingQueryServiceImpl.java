package com.trazza.trazzawebservice.matchmaking.application.internal.queryservices;

import com.trazza.trazzawebservice.iam.interfaces.acl.ContactSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.matchmaking.application.internal.MatchingRules;
import com.trazza.trazzawebservice.matchmaking.application.internal.MatchmakingViewBuilder;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.MatchProposal;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ReturnRoute;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ShipmentRequest;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.ReturnRouteStatus;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.ShipmentRequestStatus;
import com.trazza.trazzawebservice.matchmaking.domain.services.MatchProposalView;
import com.trazza.trazzawebservice.matchmaking.domain.services.MatchmakingQueryService;
import com.trazza.trazzawebservice.matchmaking.domain.services.MessageView;
import com.trazza.trazzawebservice.matchmaking.domain.services.ReturnRouteView;
import com.trazza.trazzawebservice.matchmaking.domain.services.ShipmentRequestView;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.MatchProposalRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.MessageRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.ReturnRouteRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.ShipmentRequestRepository;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class MatchmakingQueryServiceImpl implements MatchmakingQueryService {

    private final ReturnRouteRepository returnRouteRepository;
    private final ShipmentRequestRepository shipmentRequestRepository;
    private final MatchProposalRepository matchProposalRepository;
    private final MessageRepository messageRepository;
    private final IamContextFacade iamContextFacade;
    private final MatchmakingViewBuilder viewBuilder;

    public MatchmakingQueryServiceImpl(ReturnRouteRepository returnRouteRepository,
                                       ShipmentRequestRepository shipmentRequestRepository,
                                       MatchProposalRepository matchProposalRepository,
                                       MessageRepository messageRepository, IamContextFacade iamContextFacade,
                                       MatchmakingViewBuilder viewBuilder) {
        this.returnRouteRepository = returnRouteRepository;
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.matchProposalRepository = matchProposalRepository;
        this.messageRepository = messageRepository;
        this.iamContextFacade = iamContextFacade;
        this.viewBuilder = viewBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRouteView> listMyRoutes(Long userId) {
        return iamContextFacade.findCarrierProfileId(userId)
                .map(id -> viewBuilder.buildRoutes(returnRouteRepository.findByCarrierProfileIdOrderByDepartureAtDesc(id)))
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ReturnRouteView> getRoute(Long userId, UUID routeId) {
        var carrierProfileId = iamContextFacade.findCarrierProfileId(userId).orElse(null);
        var route = returnRouteRepository.findByPublicId(routeId).orElse(null);
        if (route == null || !Objects.equals(route.getCarrierProfileId(), carrierProfileId))
            return Result.failure(CommandError.notFound("route-not-found"));
        return Result.success(viewBuilder.buildRoute(route));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<ShipmentRequestView>> suggestShipments(Long userId, UUID routeId) {
        var routeResult = getRoute(userId, routeId);
        if (routeResult.isFailure()) return routeResult.asFailure();
        var route = routeResult.value();
        if (!route.route().isOpenForProposals()) return Result.success(List.of());

        var origin = route.route().getOrigin();
        var destination = route.route().getDestination();
        var candidates = openShipments().stream()
                .filter(shipment -> MatchingRules.fits(route, shipment))
                .sorted(Comparator.comparingDouble(shipment -> shipment.getPickup().distanceToSegment(origin, destination)))
                .toList();
        return Result.success(viewBuilder.buildShipments(candidates));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentRequestView> listMyShipments(Long userId) {
        return iamContextFacade.findMerchantProfileId(userId)
                .map(id -> viewBuilder.buildShipments(shipmentRequestRepository.findByMerchantProfileIdOrderByCreatedAtDesc(id)))
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentRequestView> listOpenShipments() {
        return viewBuilder.buildShipments(openShipments());
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<ReturnRouteView>> suggestRoutes(Long userId, UUID shipmentId) {
        var merchantProfileId = iamContextFacade.findMerchantProfileId(userId).orElse(null);
        var shipment = shipmentRequestRepository.findByPublicId(shipmentId).orElse(null);
        if (shipment == null || !Objects.equals(shipment.getMerchantProfileId(), merchantProfileId))
            return Result.failure(CommandError.notFound("shipment-not-found"));
        if (shipment.getStatus() != ShipmentRequestStatus.OPEN) return Result.success(List.of());

        var routes = returnRouteRepository.findByStatusAndDepartureAtBetween(ReturnRouteStatus.PUBLISHED,
                shipment.getPickupFrom().minus(MatchingRules.EARLY_DEPARTURE), shipment.getPickupUntil());
        return Result.success(viewBuilder.buildRoutes(routes).stream()
                .filter(route -> MatchingRules.fits(route, shipment))
                .sorted(Comparator.comparing(route -> route.route().getDepartureAt()))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchProposalView> listMyProposals(Long userId) {
        var proposals = new ArrayList<MatchProposal>();
        iamContextFacade.findCarrierProfileId(userId).ifPresent(id -> proposals.addAll(
                matchProposalRepository.findByReturnRouteIdInOrderByCreatedAtDesc(
                        returnRouteRepository.findByCarrierProfileIdOrderByDepartureAtDesc(id).stream()
                                .map(ReturnRoute::getId).toList())));
        iamContextFacade.findMerchantProfileId(userId).ifPresent(id -> proposals.addAll(
                matchProposalRepository.findByShipmentRequestIdInOrderByCreatedAtDesc(
                        shipmentRequestRepository.findByMerchantProfileIdOrderByCreatedAtDesc(id).stream()
                                .map(ShipmentRequest::getId).toList())));

        var distinct = new LinkedHashMap<Long, MatchProposal>();
        proposals.forEach(proposal -> distinct.putIfAbsent(proposal.getId(), proposal));
        return viewBuilder.buildProposals(distinct.values().stream()
                .sorted(Comparator.comparing(MatchProposal::getCreatedAt).reversed())
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Result<MatchProposalView> getProposal(Long userId, UUID proposalId) {
        var proposal = matchProposalRepository.findByPublicId(proposalId).orElse(null);
        if (proposal == null || !viewBuilder.findParticipants(proposal).includes(userId))
            return Result.failure(CommandError.notFound("proposal-not-found"));
        return Result.success(viewBuilder.buildProposal(proposal));
    }

    @Override
    @Transactional
    public Result<List<MessageView>> listMessages(Long userId, UUID proposalId) {
        var proposal = matchProposalRepository.findByPublicId(proposalId).orElse(null);
        if (proposal == null) return Result.failure(CommandError.notFound("proposal-not-found"));
        var participants = viewBuilder.findParticipants(proposal);
        if (!participants.includes(userId)) return Result.failure(CommandError.notFound("proposal-not-found"));

        var messages = messageRepository.findByMatchProposalIdOrderBySentAt(proposal.getId());
        var now = Instant.now();
        messages.stream().filter(message -> !Objects.equals(message.getSenderUserId(), userId) && message.getReadAt() == null)
                .forEach(message -> message.markRead(now));

        var carrier = iamContextFacade.findContact(participants.carrierUserId()).orElse(null);
        var merchant = iamContextFacade.findContact(participants.merchantUserId()).orElse(null);
        return Result.success(messages.stream().map(message -> {
            ContactSnapshot sender = Objects.equals(message.getSenderUserId(), participants.carrierUserId())
                    ? carrier : merchant;
            return new MessageView(message, sender == null ? null : sender.publicId(),
                    sender == null ? "" : sender.fullName(), Objects.equals(message.getSenderUserId(), userId));
        }).toList());
    }

    private List<ShipmentRequest> openShipments() {
        return shipmentRequestRepository.findByStatusAndPickupUntilAfterOrderByPickupFrom(ShipmentRequestStatus.OPEN,
                Instant.now());
    }
}
