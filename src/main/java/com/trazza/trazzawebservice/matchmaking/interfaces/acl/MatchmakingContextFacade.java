package com.trazza.trazzawebservice.matchmaking.interfaces.acl;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Anti-corruption layer used by Service Execution to read and update Matchmaking. It depends only on Matchmaking
 * repositories, so it never loops back into Execution.
 */
public interface MatchmakingContextFacade {

    List<TripContextSnapshot> listTripContexts(Collection<Long> matchProposalIds);

    /** Accepted or withdrawn proposals of the carrier's routes and of the merchant's shipments. */
    List<Long> listProposalIdsWithTrips(Long carrierProfileId, Long merchantProfileId);

    /** The cargo was picked up: shipment IN_TRANSIT, route IN_PROGRESS. */
    void markPickedUp(Long matchProposalId);

    /** The cargo was delivered: shipment DELIVERED; route COMPLETED when nothing else is pending on it. */
    void markDelivered(Long matchProposalId);

    /**
     * The trip was cancelled: the proposal is withdrawn, the shipment is cancelled (merchant) or reopened (carrier),
     * and the route slot is released.
     */
    void release(Long matchProposalId, boolean cancelledByMerchant, String reason, Instant now);

    /** Deletes the chat of the proposal once its trip is closed. */
    int purgeChat(Long matchProposalId);
}
