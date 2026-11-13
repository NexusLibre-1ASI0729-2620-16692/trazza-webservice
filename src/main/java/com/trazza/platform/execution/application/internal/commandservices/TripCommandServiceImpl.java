package com.trazza.platform.execution.application.internal.commandservices;

import com.trazza.platform.billing.interfaces.acl.BillingContextFacade;
import com.trazza.platform.execution.application.internal.TripAccess;
import com.trazza.platform.execution.application.internal.TripAccessService;
import com.trazza.platform.execution.domain.model.aggregates.Trip;
import com.trazza.platform.execution.domain.model.commands.AssignDriverCommand;
import com.trazza.platform.execution.domain.model.commands.CancelTripCommand;
import com.trazza.platform.execution.domain.model.commands.ConfirmTripCommand;
import com.trazza.platform.execution.domain.model.commands.DeliverTripCommand;
import com.trazza.platform.execution.domain.model.commands.PickUpTripCommand;
import com.trazza.platform.execution.domain.services.TripCommandService;
import com.trazza.platform.execution.domain.services.TripView;
import com.trazza.platform.execution.infrastructure.persistence.jpa.repositories.TripRepository;
import com.trazza.platform.iam.interfaces.acl.IamContextFacade;
import com.trazza.platform.matchmaking.interfaces.acl.MatchmakingContextFacade;
import com.trazza.platform.reputation.interfaces.acl.ReputationContextFacade;
import com.trazza.platform.shared.domain.model.CommandError;
import com.trazza.platform.shared.domain.model.Result;
import com.trazza.platform.shared.infrastructure.transactions.Transactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Trip life cycle. Delivering opens the freight settlement, confirming awards points and deletes the chat, and a
 * merchant who cancels after the route left owes lost profit (30% of the agreed rate) to the carrier.
 */
@Service
public class TripCommandServiceImpl implements TripCommandService {

    public static final BigDecimal LOST_PROFIT_RATE = new BigDecimal("0.30");

    private final TripAccessService tripAccessService;
    private final TripRepository tripRepository;
    private final MatchmakingContextFacade matchmakingContextFacade;
    private final IamContextFacade iamContextFacade;
    private final BillingContextFacade billingContextFacade;
    private final ReputationContextFacade reputationContextFacade;

    public TripCommandServiceImpl(TripAccessService tripAccessService, TripRepository tripRepository,
                                  MatchmakingContextFacade matchmakingContextFacade, IamContextFacade iamContextFacade,
                                  BillingContextFacade billingContextFacade,
                                  ReputationContextFacade reputationContextFacade) {
        this.tripAccessService = tripAccessService;
        this.tripRepository = tripRepository;
        this.matchmakingContextFacade = matchmakingContextFacade;
        this.iamContextFacade = iamContextFacade;
        this.billingContextFacade = billingContextFacade;
        this.reputationContextFacade = reputationContextFacade;
    }

    @Override
    @Transactional
    public Result<TripView> handle(AssignDriverCommand command) {
        var access = tripAccessService.find(command.userId(), command.tripId()).orElse(null);
        if (access == null) return Result.failure(CommandError.notFound("trip-not-found"));
        if (!access.isCarrier()) return Result.failure(CommandError.forbidden("only-carrier"));

        var driver = command.driverId() == null ? null : iamContextFacade.findDriver(command.driverId()).orElse(null);
        if (driver == null || !Objects.equals(driver.carrierProfileId(), access.context().carrierProfileId()))
            return Result.failure(CommandError.notFound("driver-not-found"));
        if (!driver.active()) return Result.failure(CommandError.conflict("driver-inactive"));

        return save(access, access.trip().assignDriver(driver.id()));
    }

    @Override
    @Transactional
    public Result<TripView> handle(PickUpTripCommand command) {
        var access = tripAccessService.find(command.userId(), command.tripId()).orElse(null);
        if (access == null) return Result.failure(CommandError.notFound("trip-not-found"));
        if (!access.isCarrier()) return Result.failure(CommandError.forbidden("only-carrier"));

        var result = access.trip().pickUp(command.userId(), Instant.now());
        if (result.isSuccess()) matchmakingContextFacade.markPickedUp(access.trip().getMatchProposalId());
        return save(access, result);
    }

    @Override
    @Transactional
    public Result<TripView> handle(DeliverTripCommand command) {
        var access = tripAccessService.find(command.userId(), command.tripId()).orElse(null);
        if (access == null) return Result.failure(CommandError.notFound("trip-not-found"));
        if (!access.isCarrier()) return Result.failure(CommandError.forbidden("only-carrier"));

        var trip = access.trip();
        var result = trip.deliver(command.userId(), Instant.now());
        if (result.isSuccess()) {
            matchmakingContextFacade.markDelivered(trip.getMatchProposalId());
            billingContextFacade.createFreightSettlement(trip.getId(), trip.getAgreedRate(), trip.getCurrency());
        }
        return Transactions.rollbackOnFailure(save(access, result));
    }

    @Override
    @Transactional
    public Result<TripView> handle(ConfirmTripCommand command) {
        var access = tripAccessService.find(command.userId(), command.tripId()).orElse(null);
        if (access == null) return Result.failure(CommandError.notFound("trip-not-found"));
        if (!access.isMerchant()) return Result.failure(CommandError.forbidden("only-merchant"));

        var now = Instant.now();
        var trip = access.trip();
        var result = trip.confirm(command.userId(), now);
        if (result.isSuccess()) {
            reputationContextFacade.awardTripCompleted(access.carrierUserId(), trip.getId(), now);
            reputationContextFacade.awardTripCompleted(access.merchantUserId(), trip.getId(), now);
            matchmakingContextFacade.purgeChat(trip.getMatchProposalId());
        }
        return Transactions.rollbackOnFailure(save(access, result));
    }

    @Override
    @Transactional
    public Result<TripView> handle(CancelTripCommand command) {
        var access = tripAccessService.find(command.userId(), command.tripId()).orElse(null);
        if (access == null) return Result.failure(CommandError.notFound("trip-not-found"));

        var now = Instant.now();
        var trip = access.trip();
        var result = trip.cancel(command.userId(), command.reason(), now);
        if (result.isSuccess()) {
            matchmakingContextFacade.release(trip.getMatchProposalId(), access.isMerchant(), command.reason(), now);
            if (access.isMerchant() && !now.isBefore(access.context().departureAt()))
                billingContextFacade.createCancellationCompensation(trip.getId(),
                        trip.getAgreedRate().multiply(LOST_PROFIT_RATE).setScale(2, RoundingMode.HALF_UP),
                        trip.getCurrency(), now);
            matchmakingContextFacade.purgeChat(trip.getMatchProposalId());
        }
        return Transactions.rollbackOnFailure(save(access, result));
    }

    private Result<TripView> save(TripAccess access, Result<Trip> result) {
        if (result.isFailure()) return result.asFailure();
        tripRepository.saveAndFlush(access.trip());
        return Result.success(tripAccessService.buildView(access));
    }
}
