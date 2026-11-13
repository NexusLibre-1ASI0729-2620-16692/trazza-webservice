package com.trazza.platform.billing.application.internal.commandservices;

import com.trazza.platform.billing.domain.model.commands.ConfirmFreightReceivedCommand;
import com.trazza.platform.billing.domain.model.commands.MarkFreightPaidCommand;
import com.trazza.platform.billing.domain.model.commands.UpdateCompensationCommand;
import com.trazza.platform.billing.domain.services.CompensationView;
import com.trazza.platform.billing.domain.services.FreightSettlementView;
import com.trazza.platform.billing.domain.services.SettlementCommandService;
import com.trazza.platform.billing.infrastructure.persistence.jpa.repositories.CancellationCompensationRepository;
import com.trazza.platform.billing.infrastructure.persistence.jpa.repositories.FreightSettlementRepository;
import com.trazza.platform.execution.interfaces.acl.ExecutionContextFacade;
import com.trazza.platform.execution.interfaces.acl.TripSnapshot;
import com.trazza.platform.shared.domain.model.CommandError;
import com.trazza.platform.shared.domain.model.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class SettlementCommandServiceImpl implements SettlementCommandService {

    private static final int MAX_REFERENCE_LENGTH = 100;

    private final FreightSettlementRepository settlementRepository;
    private final CancellationCompensationRepository compensationRepository;
    private final ExecutionContextFacade executionContextFacade;

    public SettlementCommandServiceImpl(FreightSettlementRepository settlementRepository,
                                        CancellationCompensationRepository compensationRepository,
                                        ExecutionContextFacade executionContextFacade) {
        this.settlementRepository = settlementRepository;
        this.compensationRepository = compensationRepository;
        this.executionContextFacade = executionContextFacade;
    }

    @Override
    @Transactional
    public Result<FreightSettlementView> handle(MarkFreightPaidCommand command) {
        var trip = findTripOf(command.userId(), command.tripId());
        if (trip == null) return Result.failure(CommandError.notFound("trip-not-found"));
        if (!Objects.equals(trip.merchantUserId(), command.userId()))
            return Result.failure(CommandError.forbidden("only-merchant"));
        if (command.externalReference() != null && command.externalReference().trim().length() > MAX_REFERENCE_LENGTH)
            return Result.failure(CommandError.validation("external-reference-invalid"));

        var settlement = settlementRepository.findById(trip.id()).orElse(null);
        if (settlement == null) return Result.failure(CommandError.notFound("settlement-not-found"));
        settlement.markPaid(command.externalReference(), Instant.now());
        settlementRepository.saveAndFlush(settlement);
        return Result.success(new FreightSettlementView(settlement, trip, false));
    }

    @Override
    @Transactional
    public Result<FreightSettlementView> handle(ConfirmFreightReceivedCommand command) {
        var trip = findTripOf(command.userId(), command.tripId());
        if (trip == null) return Result.failure(CommandError.notFound("trip-not-found"));
        if (!Objects.equals(trip.carrierUserId(), command.userId()))
            return Result.failure(CommandError.forbidden("only-carrier"));

        var settlement = settlementRepository.findById(trip.id()).orElse(null);
        if (settlement == null) return Result.failure(CommandError.notFound("settlement-not-found"));
        settlement.confirmReceived(Instant.now());
        settlementRepository.saveAndFlush(settlement);
        return Result.success(new FreightSettlementView(settlement, trip, true));
    }

    @Override
    @Transactional
    public Result<CompensationView> handle(UpdateCompensationCommand command) {
        var trip = findTripOf(command.userId(), command.tripId());
        if (trip == null) return Result.failure(CommandError.notFound("trip-not-found"));

        var compensation = compensationRepository.findById(trip.id()).orElse(null);
        if (compensation == null) return Result.failure(CommandError.notFound("compensation-not-found"));
        if (!compensation.isOpen()) return Result.failure(CommandError.conflict("compensation-settled"));

        var isCarrier = Objects.equals(trip.carrierUserId(), command.userId());
        var now = Instant.now();
        switch (command.action()) {
            case CONFIRM_PAID -> {
                if (!isCarrier) return Result.failure(CommandError.forbidden("only-carrier"));
                compensation.confirmPaid(now);
            }
            case WAIVE -> {
                if (!isCarrier) return Result.failure(CommandError.forbidden("only-carrier"));
                compensation.waive(now);
            }
            case DISPUTE -> {
                if (isCarrier) return Result.failure(CommandError.forbidden("only-merchant"));
                compensation.dispute();
            }
        }
        compensationRepository.saveAndFlush(compensation);
        return Result.success(new CompensationView(compensation, trip, isCarrier));
    }

    private TripSnapshot findTripOf(Long userId, UUID tripId) {
        return executionContextFacade.findTrip(tripId)
                .filter(trip -> Objects.equals(trip.merchantUserId(), userId)
                        || Objects.equals(trip.carrierUserId(), userId))
                .orElse(null);
    }
}
