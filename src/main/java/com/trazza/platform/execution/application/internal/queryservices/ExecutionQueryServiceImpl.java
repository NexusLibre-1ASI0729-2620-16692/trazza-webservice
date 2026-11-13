package com.trazza.platform.execution.application.internal.queryservices;

import com.trazza.platform.execution.application.internal.TripAccessService;
import com.trazza.platform.execution.domain.model.aggregates.DeviationAlert;
import com.trazza.platform.execution.domain.model.aggregates.Incident;
import com.trazza.platform.execution.domain.model.aggregates.TrackingPoint;
import com.trazza.platform.execution.domain.model.entities.TripStatusChange;
import com.trazza.platform.execution.domain.services.ExecutionQueryService;
import com.trazza.platform.execution.domain.services.IotDeviceView;
import com.trazza.platform.execution.domain.services.TripView;
import com.trazza.platform.execution.infrastructure.persistence.jpa.repositories.DeviationAlertRepository;
import com.trazza.platform.execution.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.trazza.platform.execution.infrastructure.persistence.jpa.repositories.IotDeviceRepository;
import com.trazza.platform.execution.infrastructure.persistence.jpa.repositories.TrackingPointRepository;
import com.trazza.platform.execution.infrastructure.persistence.jpa.repositories.TripRepository;
import com.trazza.platform.iam.interfaces.acl.IamContextFacade;
import com.trazza.platform.iam.interfaces.acl.VehicleSnapshot;
import com.trazza.platform.matchmaking.interfaces.acl.MatchmakingContextFacade;
import com.trazza.platform.shared.domain.model.CommandError;
import com.trazza.platform.shared.domain.model.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExecutionQueryServiceImpl implements ExecutionQueryService {

    private final TripAccessService tripAccessService;
    private final TripRepository tripRepository;
    private final TrackingPointRepository trackingPointRepository;
    private final DeviationAlertRepository deviationAlertRepository;
    private final IncidentRepository incidentRepository;
    private final IotDeviceRepository iotDeviceRepository;
    private final MatchmakingContextFacade matchmakingContextFacade;
    private final IamContextFacade iamContextFacade;

    public ExecutionQueryServiceImpl(TripAccessService tripAccessService, TripRepository tripRepository,
                                     TrackingPointRepository trackingPointRepository,
                                     DeviationAlertRepository deviationAlertRepository,
                                     IncidentRepository incidentRepository, IotDeviceRepository iotDeviceRepository,
                                     MatchmakingContextFacade matchmakingContextFacade,
                                     IamContextFacade iamContextFacade) {
        this.tripAccessService = tripAccessService;
        this.tripRepository = tripRepository;
        this.trackingPointRepository = trackingPointRepository;
        this.deviationAlertRepository = deviationAlertRepository;
        this.incidentRepository = incidentRepository;
        this.iotDeviceRepository = iotDeviceRepository;
        this.matchmakingContextFacade = matchmakingContextFacade;
        this.iamContextFacade = iamContextFacade;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripView> listMyTrips(Long userId) {
        var proposalIds = matchmakingContextFacade.listProposalIdsWithTrips(
                iamContextFacade.findCarrierProfileId(userId).orElse(null),
                iamContextFacade.findMerchantProfileId(userId).orElse(null));
        if (proposalIds.isEmpty()) return List.of();
        var trips = tripRepository.findByMatchProposalIdIn(proposalIds);
        return tripAccessService.buildAccess(userId, trips).stream()
                .map(tripAccessService::buildView)
                .sorted(Comparator.comparing((TripView view) -> view.trip().getCreatedAt()).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Result<TripView> getTrip(Long userId, UUID tripId) {
        return tripAccessService.find(userId, tripId)
                .map(access -> Result.success(tripAccessService.buildView(access)))
                .orElseGet(() -> Result.failure(CommandError.notFound("trip-not-found")));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<TripStatusChange>> listHistory(Long userId, UUID tripId) {
        return tripAccessService.find(userId, tripId)
                .map(access -> Result.success(access.trip().getHistory().stream()
                        .sorted(Comparator.comparing(TripStatusChange::getChangedAt)
                                .thenComparing(TripStatusChange::getId))
                        .toList()))
                .orElseGet(() -> Result.failure(CommandError.notFound("trip-not-found")));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<TrackingPoint>> listTrackingPoints(Long userId, UUID tripId) {
        return tripAccessService.find(userId, tripId)
                .map(access -> Result.success(
                        trackingPointRepository.findByTripIdOrderByRecordedAtAscIdAsc(access.trip().getId())))
                .orElseGet(() -> Result.failure(CommandError.notFound("trip-not-found")));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<DeviationAlert>> listDeviationAlerts(Long userId, UUID tripId) {
        return tripAccessService.find(userId, tripId)
                .map(access -> Result.success(
                        deviationAlertRepository.findByTripIdOrderByDetectedAtDesc(access.trip().getId())))
                .orElseGet(() -> Result.failure(CommandError.notFound("trip-not-found")));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<Incident>> listIncidents(Long userId, UUID tripId) {
        return tripAccessService.find(userId, tripId)
                .map(access -> Result.success(incidentRepository.findByTripIdOrderByReportedAtDesc(access.trip().getId())))
                .orElseGet(() -> Result.failure(CommandError.notFound("trip-not-found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IotDeviceView> listMyIotDevices(Long userId) {
        var carrierProfileId = iamContextFacade.findCarrierProfileId(userId);
        if (carrierProfileId.isEmpty()) return List.of();
        var vehicles = iamContextFacade.listVehiclesOfCarrier(carrierProfileId.get()).stream()
                .collect(Collectors.toMap(VehicleSnapshot::id, Function.identity()));
        if (vehicles.isEmpty()) return List.of();
        return iotDeviceRepository.findByAssignedVehicleIdIn(vehicles.keySet()).stream().map(device -> {
            var vehicle = vehicles.get(device.getAssignedVehicleId());
            return new IotDeviceView(device, vehicle.publicId(), vehicle.plate());
        }).toList();
    }
}
