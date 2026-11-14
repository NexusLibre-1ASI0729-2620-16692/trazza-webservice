package com.trazza.trazzawebservice.execution.application.internal;

import com.trazza.trazzawebservice.execution.domain.model.aggregates.Trip;
import com.trazza.trazzawebservice.execution.domain.services.TripView;
import com.trazza.trazzawebservice.execution.infrastructure.persistence.jpa.repositories.DeviationAlertRepository;
import com.trazza.trazzawebservice.execution.infrastructure.persistence.jpa.repositories.TrackingPointRepository;
import com.trazza.trazzawebservice.execution.infrastructure.persistence.jpa.repositories.TripRepository;
import com.trazza.trazzawebservice.iam.interfaces.acl.ContactSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.DriverSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.matchmaking.interfaces.acl.MatchmakingContextFacade;
import com.trazza.trazzawebservice.matchmaking.interfaces.acl.TripContextSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Loads trips only for their carrier or merchant, and builds {@link TripView}. */
@Component
public class TripAccessService {

    private final TripRepository tripRepository;
    private final TrackingPointRepository trackingPointRepository;
    private final DeviationAlertRepository deviationAlertRepository;
    private final MatchmakingContextFacade matchmakingContextFacade;
    private final IamContextFacade iamContextFacade;

    public TripAccessService(TripRepository tripRepository, TrackingPointRepository trackingPointRepository,
                             DeviationAlertRepository deviationAlertRepository,
                             MatchmakingContextFacade matchmakingContextFacade, IamContextFacade iamContextFacade) {
        this.tripRepository = tripRepository;
        this.trackingPointRepository = trackingPointRepository;
        this.deviationAlertRepository = deviationAlertRepository;
        this.matchmakingContextFacade = matchmakingContextFacade;
        this.iamContextFacade = iamContextFacade;
    }

    /** Empty when the trip does not exist or the user is not part of it. */
    public Optional<TripAccess> find(Long userId, UUID tripId) {
        if (tripId == null) return Optional.empty();
        return tripRepository.findByPublicId(tripId)
                .flatMap(trip -> buildAccess(userId, List.of(trip)).stream().findFirst())
                .filter(access -> access.isCarrier() || access.isMerchant());
    }

    public List<TripAccess> buildAccess(Long userId, List<Trip> trips) {
        var contexts = matchmakingContextFacade.listTripContexts(trips.stream().map(Trip::getMatchProposalId).toList())
                .stream().collect(Collectors.toMap(TripContextSnapshot::matchProposalId, Function.identity()));
        var result = new ArrayList<TripAccess>();
        for (var trip : trips) {
            var context = contexts.get(trip.getMatchProposalId());
            if (context == null) continue;
            var carrierUserId = iamContextFacade.findUserIdByCarrierProfileId(context.carrierProfileId()).orElse(0L);
            var merchantUserId = iamContextFacade.findUserIdByMerchantProfileId(context.merchantProfileId()).orElse(0L);
            result.add(new TripAccess(trip, context, carrierUserId, merchantUserId, userId));
        }
        return result;
    }

    public TripView buildView(TripAccess access) {
        var trip = access.trip();
        var context = access.context();
        var carrier = iamContextFacade.findContact(access.carrierUserId()).orElse(null);
        var merchant = iamContextFacade.findContact(access.merchantUserId()).orElse(null);
        var vehicle = iamContextFacade.listVehicles(List.of(context.vehicleId())).stream().findFirst().orElse(null);
        var driverId = trip.getDriverId() != null ? trip.getDriverId() : context.routeDriverId();
        var driverName = driverId == null ? null
                : iamContextFacade.findDriver(driverId).map(DriverSnapshot::fullName).orElse(null);
        var lastPoint = trackingPointRepository.findFirstByTripIdOrderByRecordedAtDescIdDesc(trip.getId()).orElse(null);
        var openAlerts = (int) deviationAlertRepository.countByTripIdAndAcknowledgedAtIsNull(trip.getId());

        return new TripView(trip, context, access.carrierUserId(), carrier == null ? "" : carrier.fullName(),
                carrier == null ? null : carrier.phone(), access.merchantUserId(),
                merchant == null ? "" : merchant.displayName(),
                Optional.ofNullable(merchant).map(ContactSnapshot::phone).orElse(null),
                vehicle == null ? null : vehicle.publicId(), vehicle == null ? "" : vehicle.plate(), driverName,
                lastPoint, openAlerts, access.isCarrier());
    }
}
