package com.trazza.trazzawebservice.execution.application.acl;

import com.trazza.trazzawebservice.execution.domain.model.aggregates.IotDevice;
import com.trazza.trazzawebservice.execution.domain.model.aggregates.Trip;
import com.trazza.trazzawebservice.execution.domain.model.valueobjects.IotDeviceStatus;
import com.trazza.trazzawebservice.execution.infrastructure.persistence.jpa.repositories.IotDeviceRepository;
import com.trazza.trazzawebservice.execution.infrastructure.persistence.jpa.repositories.TripRepository;
import com.trazza.trazzawebservice.execution.interfaces.acl.ExecutionContextFacade;
import com.trazza.trazzawebservice.execution.interfaces.acl.TripSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.matchmaking.interfaces.acl.MatchmakingContextFacade;
import com.trazza.trazzawebservice.matchmaking.interfaces.acl.TripContextSnapshot;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExecutionContextFacadeImpl implements ExecutionContextFacade {

    private final TripRepository tripRepository;
    private final IotDeviceRepository iotDeviceRepository;
    private final MatchmakingContextFacade matchmakingContextFacade;
    private final IamContextFacade iamContextFacade;

    public ExecutionContextFacadeImpl(TripRepository tripRepository, IotDeviceRepository iotDeviceRepository,
                                      MatchmakingContextFacade matchmakingContextFacade,
                                      IamContextFacade iamContextFacade) {
        this.tripRepository = tripRepository;
        this.iotDeviceRepository = iotDeviceRepository;
        this.matchmakingContextFacade = matchmakingContextFacade;
        this.iamContextFacade = iamContextFacade;
    }

    @Override
    @Transactional
    public UUID createTrip(Long matchProposalId, Long driverId, BigDecimal agreedRate, String currency,
                           Long acceptedByUserId) {
        var trip = tripRepository.saveAndFlush(
                new Trip(matchProposalId, driverId, agreedRate, currency, acceptedByUserId, Instant.now()));
        return trip.getPublicId();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, UUID> findTripPublicIdsByProposalIds(Collection<Long> matchProposalIds) {
        if (matchProposalIds.isEmpty()) return Map.of();
        return tripRepository.findByMatchProposalIdIn(matchProposalIds).stream()
                .collect(Collectors.toMap(Trip::getMatchProposalId, Trip::getPublicId, (first, second) -> first));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTripClosed(Long matchProposalId) {
        return tripRepository.findByMatchProposalId(matchProposalId).map(Trip::isClosed).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TripSnapshot> findTrip(UUID tripPublicId) {
        if (tripPublicId == null) return Optional.empty();
        return tripRepository.findByPublicId(tripPublicId).flatMap(trip -> toSnapshots(List.of(trip)).stream().findFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripSnapshot> listTrips(Collection<Long> tripIds) {
        if (tripIds.isEmpty()) return List.of();
        return toSnapshots(tripRepository.findAllById(new LinkedHashSet<>(tripIds)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripSnapshot> listTripsOfUser(Long userId) {
        var proposalIds = matchmakingContextFacade.listProposalIdsWithTrips(
                iamContextFacade.findCarrierProfileId(userId).orElse(null),
                iamContextFacade.findMerchantProfileId(userId).orElse(null));
        if (proposalIds.isEmpty()) return List.of();
        return toSnapshots(tripRepository.findByMatchProposalIdIn(proposalIds));
    }

    @Override
    @Transactional
    public boolean loanIotDevices(Collection<Long> vehicleIds, Instant now) {
        var alreadyLoaned = iotDeviceRepository.findByAssignedVehicleIdIn(vehicleIds).stream()
                .map(IotDevice::getAssignedVehicleId).collect(Collectors.toSet());
        var missing = new LinkedHashSet<>(vehicleIds).stream().filter(id -> !alreadyLoaned.contains(id)).toList();
        if (missing.isEmpty()) return true;

        var available = iotDeviceRepository.findByStatusAndAssignedVehicleIdIsNullOrderById(IotDeviceStatus.AVAILABLE,
                Limit.of(missing.size()));
        if (available.size() < missing.size()) return false;
        for (var i = 0; i < missing.size(); i++) available.get(i).loanTo(missing.get(i), now);
        iotDeviceRepository.saveAll(available);
        return true;
    }

    @Override
    @Transactional
    public void returnIotDevices(Collection<Long> vehicleIds) {
        if (vehicleIds.isEmpty()) return;
        var devices = iotDeviceRepository.findByAssignedVehicleIdIn(vehicleIds);
        devices.forEach(IotDevice::returnDevice);
        iotDeviceRepository.saveAll(devices);
    }

    private List<TripSnapshot> toSnapshots(List<Trip> trips) {
        if (trips.isEmpty()) return List.of();
        var contexts = matchmakingContextFacade.listTripContexts(trips.stream().map(Trip::getMatchProposalId).toList())
                .stream().collect(Collectors.toMap(TripContextSnapshot::matchProposalId, Function.identity()));
        var snapshots = new ArrayList<TripSnapshot>();
        for (var trip : trips) {
            var context = contexts.get(trip.getMatchProposalId());
            if (context == null) continue;
            var carrierUserId = iamContextFacade.findUserIdByCarrierProfileId(context.carrierProfileId()).orElse(0L);
            var merchantUserId = iamContextFacade.findUserIdByMerchantProfileId(context.merchantProfileId()).orElse(0L);
            snapshots.add(new TripSnapshot(trip.getId(), trip.getPublicId(), trip.getStatus().name(),
                    trip.getAgreedRate(), trip.getCurrency(), carrierUserId, merchantUserId, trip.getDeliveredAt(),
                    trip.getConfirmedAt(), trip.getCancelledAt(), context.originAddress(),
                    context.destinationAddress()));
        }
        return snapshots;
    }
}
