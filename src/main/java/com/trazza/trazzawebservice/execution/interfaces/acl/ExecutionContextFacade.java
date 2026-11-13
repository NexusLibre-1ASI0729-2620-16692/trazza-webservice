package com.trazza.trazzawebservice.execution.interfaces.acl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Anti-corruption layer used by Matchmaking, Billing and Reputation to open and read trips and to lend IoT devices.
 * It depends only on Execution repositories and on the IAM and Matchmaking facades, which never call back.
 */
public interface ExecutionContextFacade {

    /** Opens and saves the trip of an accepted proposal (status SCHEDULED) and returns its public id. */
    UUID createTrip(Long matchProposalId, Long driverId, BigDecimal agreedRate, String currency, Long acceptedByUserId);

    Map<Long, UUID> findTripPublicIdsByProposalIds(Collection<Long> matchProposalIds);

    /** True when the trip of the proposal was confirmed or cancelled (its chat is closed). */
    boolean isTripClosed(Long matchProposalId);

    Optional<TripSnapshot> findTrip(UUID tripPublicId);

    List<TripSnapshot> listTrips(Collection<Long> tripIds);

    /** Trips where the user is the carrier or the merchant. */
    List<TripSnapshot> listTripsOfUser(Long userId);

    /** Lends one available IoT device to each vehicle. False when there are not enough devices. */
    boolean loanIotDevices(Collection<Long> vehicleIds, Instant now);

    void returnIotDevices(Collection<Long> vehicleIds);
}
