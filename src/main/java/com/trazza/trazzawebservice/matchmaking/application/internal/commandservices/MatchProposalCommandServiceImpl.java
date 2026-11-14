package com.trazza.trazzawebservice.matchmaking.application.internal.commandservices;

import com.trazza.trazzawebservice.execution.interfaces.acl.ExecutionContextFacade;
import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.matchmaking.application.internal.MatchingRules;
import com.trazza.trazzawebservice.matchmaking.application.internal.MatchmakingViewBuilder;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.MatchProposal;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.Message;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ReturnRoute;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ShipmentRequest;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.AcceptMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CancelMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CreateMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.RejectMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.SendMessageCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.MatchProposalStatus;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.ShipmentRequestStatus;
import com.trazza.trazzawebservice.matchmaking.domain.services.MatchProposalCommandService;
import com.trazza.trazzawebservice.matchmaking.domain.services.MatchProposalView;
import com.trazza.trazzawebservice.matchmaking.domain.services.MessageView;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.MatchProposalRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.MessageRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.ReturnRouteRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.ShipmentRequestRepository;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.Money;
import com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa.RowLocks;
import com.trazza.trazzawebservice.shared.infrastructure.transactions.Transactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Single offer flow: the carrier offers a rate for a shipment on one of its routes, the merchant accepts or rejects
 * it. Accepting locks the route, re-checks capacity and opens the trip.
 */
@Service
public class MatchProposalCommandServiceImpl implements MatchProposalCommandService {

    private final ReturnRouteRepository returnRouteRepository;
    private final ShipmentRequestRepository shipmentRequestRepository;
    private final MatchProposalRepository matchProposalRepository;
    private final MessageRepository messageRepository;
    private final IamContextFacade iamContextFacade;
    private final ExecutionContextFacade executionContextFacade;
    private final MatchmakingViewBuilder viewBuilder;
    private final RowLocks rowLocks;

    public MatchProposalCommandServiceImpl(ReturnRouteRepository returnRouteRepository,
                                           ShipmentRequestRepository shipmentRequestRepository,
                                           MatchProposalRepository matchProposalRepository,
                                           MessageRepository messageRepository, IamContextFacade iamContextFacade,
                                           ExecutionContextFacade executionContextFacade,
                                           MatchmakingViewBuilder viewBuilder, RowLocks rowLocks) {
        this.returnRouteRepository = returnRouteRepository;
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.matchProposalRepository = matchProposalRepository;
        this.messageRepository = messageRepository;
        this.iamContextFacade = iamContextFacade;
        this.executionContextFacade = executionContextFacade;
        this.viewBuilder = viewBuilder;
        this.rowLocks = rowLocks;
    }

    @Override
    @Transactional
    public Result<MatchProposalView> handle(CreateMatchProposalCommand command) {
        var carrierProfileId = iamContextFacade.findCarrierProfileId(command.userId());
        if (carrierProfileId.isEmpty()) return Result.failure(CommandError.forbidden("carrier-profile-required"));

        var route = command.returnRouteId() == null ? null
                : returnRouteRepository.findByPublicId(command.returnRouteId()).orElse(null);
        if (route == null || !Objects.equals(route.getCarrierProfileId(), carrierProfileId.get()))
            return Result.failure(CommandError.notFound("route-not-found"));
        if (!route.isOpenForProposals()) return Result.failure(CommandError.conflict("route-not-accepting"));

        var shipment = command.shipmentRequestId() == null ? null
                : shipmentRequestRepository.findByPublicId(command.shipmentRequestId()).orElse(null);
        if (shipment == null) return Result.failure(CommandError.notFound("shipment-not-found"));
        if (shipment.getStatus() != ShipmentRequestStatus.OPEN)
            return Result.failure(CommandError.conflict("shipment-not-open"));

        if (matchProposalRepository.existsByShipmentRequestIdAndReturnRouteId(shipment.getId(), route.getId()))
            return Result.failure(CommandError.conflict("proposal-already-exists"));

        if (command.offeredRate() == null || command.offeredRate().signum() <= 0)
            return Result.failure(CommandError.validation("rate-invalid"));
        var rate = Money.create(command.offeredRate(), command.currency() == null ? Money.SOLES : command.currency());
        if (rate.isFailure()) return rate.asFailure();

        if (!MatchingRules.fitsSchedule(route, shipment))
            return Result.failure(CommandError.conflict("schedule-incompatible"));
        if (!MatchingRules.fitsCapacity(viewBuilder.buildRoute(route), shipment))
            return Result.failure(CommandError.conflict("capacity-exceeded"));

        var proposal = matchProposalRepository.saveAndFlush(new MatchProposal(shipment.getId(), route.getId(),
                rate.value().amount(), rate.value().currency()));
        return Result.success(viewBuilder.buildProposal(proposal));
    }

    @Override
    @Transactional
    public Result<MatchProposalView> handle(AcceptMatchProposalCommand command) {
        return Transactions.rollbackOnFailure(accept(command));
    }

    @Override
    @Transactional
    public Result<MatchProposalView> handle(RejectMatchProposalCommand command) {
        var proposal = matchProposalRepository.findByPublicId(command.matchProposalId()).orElse(null);
        if (proposal == null || !Objects.equals(viewBuilder.findParticipants(proposal).merchantUserId(), command.userId()))
            return Result.failure(CommandError.notFound("proposal-not-found"));
        if (!proposal.isPending()) return Result.failure(CommandError.conflict("proposal-not-pending"));

        proposal.reject(Instant.now());
        matchProposalRepository.saveAndFlush(proposal);
        return Result.success(viewBuilder.buildProposal(proposal));
    }

    @Override
    @Transactional
    public Result<MatchProposalView> handle(CancelMatchProposalCommand command) {
        var proposal = matchProposalRepository.findByPublicId(command.matchProposalId()).orElse(null);
        if (proposal == null || !Objects.equals(viewBuilder.findParticipants(proposal).carrierUserId(), command.userId()))
            return Result.failure(CommandError.notFound("proposal-not-found"));
        if (!proposal.isPending()) return Result.failure(CommandError.conflict("proposal-not-pending"));

        proposal.cancel(Instant.now());
        matchProposalRepository.saveAndFlush(proposal);
        return Result.success(viewBuilder.buildProposal(proposal));
    }

    @Override
    @Transactional
    public Result<MessageView> handle(SendMessageCommand command) {
        var proposal = matchProposalRepository.findByPublicId(command.matchProposalId()).orElse(null);
        if (proposal == null || !viewBuilder.findParticipants(proposal).includes(command.userId()))
            return Result.failure(CommandError.notFound("proposal-not-found"));

        var chatOpen = proposal.getStatus() == MatchProposalStatus.PENDING
                || (proposal.getStatus() == MatchProposalStatus.ACCEPTED
                && !executionContextFacade.isTripClosed(proposal.getId()));
        if (!chatOpen) return Result.failure(CommandError.conflict("chat-closed"));

        if (command.content() == null || command.content().isBlank()
                || command.content().trim().length() > Message.MAX_LENGTH)
            return Result.failure(CommandError.validation("message-invalid"));

        var message = messageRepository.saveAndFlush(
                new Message(proposal.getId(), command.userId(), command.content(), Instant.now()));
        var sender = iamContextFacade.findContact(command.userId()).orElse(null);
        return Result.success(new MessageView(message, sender == null ? null : sender.publicId(),
                sender == null ? "" : sender.fullName(), true));
    }

    private Result<MatchProposalView> accept(AcceptMatchProposalCommand command) {
        var proposal = matchProposalRepository.findByPublicId(command.matchProposalId()).orElse(null);
        if (proposal == null || !Objects.equals(viewBuilder.findParticipants(proposal).merchantUserId(), command.userId()))
            return Result.failure(CommandError.notFound("proposal-not-found"));
        if (!proposal.isPending()) return Result.failure(CommandError.conflict("proposal-not-pending"));

        // Lock the route and the shipment so two merchants cannot take the last free slot at the same time.
        var route = rowLocks.lock(ReturnRoute.class, proposal.getReturnRouteId());
        var shipment = rowLocks.lock(ShipmentRequest.class, proposal.getShipmentRequestId());
        if (route == null || shipment == null) return Result.failure(CommandError.notFound("proposal-not-found"));
        if (!route.isOpenForProposals()) return Result.failure(CommandError.conflict("route-not-accepting"));
        if (shipment.getStatus() != ShipmentRequestStatus.OPEN)
            return Result.failure(CommandError.conflict("shipment-not-open"));

        var routeView = viewBuilder.buildRoute(route);
        if (!MatchingRules.fitsCapacity(routeView, shipment))
            return Result.failure(CommandError.conflict("capacity-exceeded"));

        var now = Instant.now();
        proposal.accept(now);
        shipment.markMatched();

        matchProposalRepository.findByShipmentRequestIdInOrderByCreatedAtDesc(List.of(shipment.getId())).stream()
                .filter(other -> !Objects.equals(other.getId(), proposal.getId()) && other.isPending())
                .forEach(other -> other.expire(now));

        if (routeView.acceptedShipments() + 1 >= route.getMaxShipments()) {
            route.markFull();
            matchProposalRepository.findByReturnRouteIdAndStatus(route.getId(), MatchProposalStatus.PENDING).stream()
                    .filter(other -> !Objects.equals(other.getId(), proposal.getId()))
                    .forEach(other -> other.expire(now));
        }

        matchProposalRepository.flush();
        executionContextFacade.createTrip(proposal.getId(), route.getDriverId(), proposal.getOfferedRate(),
                proposal.getCurrency(), command.userId());
        return Result.success(viewBuilder.buildProposal(proposal));
    }
}
