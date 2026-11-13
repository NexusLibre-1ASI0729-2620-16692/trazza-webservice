package com.trazza.trazzawebservice.reputation.application.acl;

import com.trazza.trazzawebservice.reputation.domain.model.aggregates.LoyaltyPointEntry;
import com.trazza.trazzawebservice.reputation.domain.model.valueobjects.LoyaltyReason;
import com.trazza.trazzawebservice.reputation.infrastructure.persistence.jpa.repositories.LoyaltyPointEntryRepository;
import com.trazza.trazzawebservice.reputation.interfaces.acl.ReputationContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReputationContextFacadeImpl implements ReputationContextFacade {

    private final LoyaltyPointEntryRepository loyaltyRepository;

    public ReputationContextFacadeImpl(LoyaltyPointEntryRepository loyaltyRepository) {
        this.loyaltyRepository = loyaltyRepository;
    }

    @Override
    @Transactional
    public void awardTripCompleted(Long userId, Long tripId, Instant now) {
        if (userId == null || userId == 0
                || loyaltyRepository.existsByUserIdAndTripIdAndReason(userId, tripId, LoyaltyReason.TRIP_COMPLETED))
            return;
        loyaltyRepository.save(new LoyaltyPointEntry(userId, tripId, LoyaltyPointEntry.TRIP_COMPLETED_POINTS,
                LoyaltyReason.TRIP_COMPLETED, now));
    }
}