package com.trazza.trazzawebservice.reputation.interfaces.acl;

import java.time.Instant;

public interface ReputationContextFacade {

    void awardTripCompleted(Long userId, Long tripId, Instant now);
}