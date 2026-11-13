package com.trazza.trazzawebservice.matchmaking.application.internal.commandservices;

import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.matchmaking.application.internal.MatchmakingViewBuilder;
import com.trazza.trazzawebservice.matchmaking.application.internal.Places;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.MatchProposal;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ShipmentRequest;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CancelShipmentRequestCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CreateShipmentRequestCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.ShipmentRequestStatus;
import com.trazza.trazzawebservice.matchmaking.domain.services.ShipmentRequestCommandService;
import com.trazza.trazzawebservice.matchmaking.domain.services.ShipmentRequestView;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.MatchProposalRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.ShipmentRequestRepository;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class ShipmentRequestCommandServiceImpl implements ShipmentRequestCommandService {

    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("30000");
    private static final BigDecimal MAX_VOLUME_M3 = new BigDecimal("99999.99");

    private final ShipmentRequestRepository shipmentRequestRepository;
    private final MatchProposalRepository matchProposalRepository;
    private final IamContextFacade iamContextFacade;
    private final MatchmakingViewBuilder viewBuilder;

    public ShipmentRequestCommandServiceImpl(ShipmentRequestRepository shipmentRequestRepository,
                                             MatchProposalRepository matchProposalRepository,
                                             IamContextFacade iamContextFacade, MatchmakingViewBuilder viewBuilder) {
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.matchProposalRepository = matchProposalRepository;
        this.iamContextFacade = iamContextFacade;
        this.viewBuilder = viewBuilder;
    }

    @Override
    @Transactional
    public Result<ShipmentRequestView> handle(CreateShipmentRequestCommand command) {
        var merchantProfileId = iamContextFacade.findMerchantProfileId(command.userId());
        if (merchantProfileId.isEmpty()) return Result.failure(CommandError.forbidden("merchant-profile-required"));

        if (command.description() == null || command.description().isBlank()
                || command.description().trim().length() > 500)
            return Result.failure(CommandError.validation("description-invalid"));
        if (!inRange(command.weightKg(), MAX_WEIGHT_KG)) return Result.failure(CommandError.validation("weight-invalid"));
        if (!inRange(command.volumeM3(), MAX_VOLUME_M3)) return Result.failure(CommandError.validation("volume-invalid"));

        var pickup = Places.validate(command.pickup());
        if (pickup.isFailure()) return pickup.asFailure();
        var delivery = Places.validate(command.delivery());
        if (delivery.isFailure()) return delivery.asFailure();

        if (command.pickupFrom() == null || command.pickupUntil() == null
                || !command.pickupUntil().isAfter(command.pickupFrom()))
            return Result.failure(CommandError.validation("pickup-window-invalid"));
        if (!command.pickupUntil().isAfter(Instant.now()))
            return Result.failure(CommandError.validation("pickup-window-past"));

        var shipment = shipmentRequestRepository.saveAndFlush(new ShipmentRequest(merchantProfileId.get(),
                command.description(), command.weightKg().setScale(2, RoundingMode.HALF_EVEN),
                command.volumeM3().setScale(2, RoundingMode.HALF_EVEN), command.pickup().address(), pickup.value(),
                command.delivery().address(), delivery.value(), command.pickupFrom(), command.pickupUntil()));
        return Result.success(viewBuilder.buildShipment(shipment));
    }

    @Override
    @Transactional
    public Result<ShipmentRequestView> handle(CancelShipmentRequestCommand command) {
        var merchantProfileId = iamContextFacade.findMerchantProfileId(command.userId()).orElse(null);
        var shipment = shipmentRequestRepository.findByPublicId(command.shipmentRequestId()).orElse(null);
        if (shipment == null || !Objects.equals(shipment.getMerchantProfileId(), merchantProfileId))
            return Result.failure(CommandError.notFound("shipment-not-found"));
        // A matched shipment already has a trip: it is cancelled from the trip, where lost profit applies.
        if (shipment.getStatus() != ShipmentRequestStatus.OPEN)
            return Result.failure(CommandError.conflict("shipment-not-open"));
        if (command.reason() == null || command.reason().isBlank() || command.reason().trim().length() > 255)
            return Result.failure(CommandError.validation("cancellation-reason-required"));

        var now = Instant.now();
        matchProposalRepository.findByShipmentRequestIdInOrderByCreatedAtDesc(List.of(shipment.getId())).stream()
                .filter(MatchProposal::isPending).forEach(p -> p.expire(now));
        shipment.cancel(command.reason(), now);
        shipmentRequestRepository.saveAndFlush(shipment);
        return Result.success(viewBuilder.buildShipment(shipment));
    }

    private static boolean inRange(BigDecimal value, BigDecimal max) {
        return value != null && value.signum() > 0 && value.compareTo(max) <= 0;
    }
}
