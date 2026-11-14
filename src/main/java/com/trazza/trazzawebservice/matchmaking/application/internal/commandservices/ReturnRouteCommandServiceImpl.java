package com.trazza.trazzawebservice.matchmaking.application.internal.commandservices;

import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.matchmaking.application.internal.MatchmakingViewBuilder;
import com.trazza.trazzawebservice.matchmaking.application.internal.Places;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.MatchProposal;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ReturnRoute;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CancelReturnRouteCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CreateReturnRouteCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.MatchProposalStatus;
import com.trazza.trazzawebservice.matchmaking.domain.services.ReturnRouteCommandService;
import com.trazza.trazzawebservice.matchmaking.domain.services.ReturnRouteView;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.MatchProposalRepository;
import com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories.ReturnRouteRepository;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class ReturnRouteCommandServiceImpl implements ReturnRouteCommandService {

    private final ReturnRouteRepository returnRouteRepository;
    private final MatchProposalRepository matchProposalRepository;
    private final IamContextFacade iamContextFacade;
    private final MatchmakingViewBuilder viewBuilder;

    public ReturnRouteCommandServiceImpl(ReturnRouteRepository returnRouteRepository,
                                         MatchProposalRepository matchProposalRepository,
                                         IamContextFacade iamContextFacade, MatchmakingViewBuilder viewBuilder) {
        this.returnRouteRepository = returnRouteRepository;
        this.matchProposalRepository = matchProposalRepository;
        this.iamContextFacade = iamContextFacade;
        this.viewBuilder = viewBuilder;
    }

    @Override
    @Transactional
    public Result<ReturnRouteView> handle(CreateReturnRouteCommand command) {
        var carrierProfileId = iamContextFacade.findCarrierProfileId(command.userId());
        if (carrierProfileId.isEmpty()) return Result.failure(CommandError.forbidden("carrier-profile-required"));

        var vehicle = command.vehicleId() == null ? null : iamContextFacade.findVehicle(command.vehicleId()).orElse(null);
        if (vehicle == null || !Objects.equals(vehicle.carrierProfileId(), carrierProfileId.get()))
            return Result.failure(CommandError.notFound("vehicle-not-found"));
        if (!vehicle.active()) return Result.failure(CommandError.conflict("vehicle-inactive"));

        if (command.driverId() != null) {
            var driver = iamContextFacade.findDriver(command.driverId()).orElse(null);
            if (driver == null || !Objects.equals(driver.carrierProfileId(), carrierProfileId.get()))
                return Result.failure(CommandError.notFound("driver-not-found"));
            if (!driver.active()) return Result.failure(CommandError.conflict("driver-inactive"));
        }

        var origin = Places.validate(command.origin());
        if (origin.isFailure()) return origin.asFailure();
        var destination = Places.validate(command.destination());
        if (destination.isFailure()) return destination.asFailure();

        if (command.departureAt() == null || !command.departureAt().isAfter(Instant.now()))
            return Result.failure(CommandError.validation("departure-must-be-future"));
        if (command.maxShipments() < 1 || command.maxShipments() > ReturnRoute.MAX_SHIPMENTS_LIMIT)
            return Result.failure(CommandError.validation("max-shipments-invalid"));

        var route = returnRouteRepository.saveAndFlush(new ReturnRoute(carrierProfileId.get(), vehicle.id(),
                command.driverId(), command.origin().address(), origin.value(), command.destination().address(),
                destination.value(), command.departureAt(), command.maxShipments()));
        return Result.success(viewBuilder.buildRoute(route));
    }

    @Override
    @Transactional
    public Result<ReturnRouteView> handle(CancelReturnRouteCommand command) {
        var carrierProfileId = iamContextFacade.findCarrierProfileId(command.userId()).orElse(null);
        var route = returnRouteRepository.findByPublicId(command.returnRouteId()).orElse(null);
        if (route == null || !Objects.equals(route.getCarrierProfileId(), carrierProfileId))
            return Result.failure(CommandError.notFound("route-not-found"));
        if (route.isClosed()) return Result.failure(CommandError.conflict("route-closed"));

        var proposals = matchProposalRepository.findByReturnRouteIdInOrderByCreatedAtDesc(List.of(route.getId()));
        if (proposals.stream().anyMatch(p -> p.getStatus() == MatchProposalStatus.ACCEPTED))
            return Result.failure(CommandError.conflict("route-has-trips"));

        var now = Instant.now();
        proposals.stream().filter(MatchProposal::isPending).forEach(p -> p.cancel(now));
        route.cancel();
        returnRouteRepository.saveAndFlush(route);
        return Result.success(viewBuilder.buildRoute(route));
    }
}
