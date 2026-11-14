package com.trazza.trazzawebservice.reputation.domain.services;

import com.trazza.trazzawebservice.shared.domain.model.Result;

import java.util.List;
import java.util.UUID;

public interface ReputationQueryService {

    List<RatingView> listReceived(Long userId);

    List<RatingView> listGiven(Long userId);

    Result<ReputationSummary> getSummary(UUID userPublicId);

    LoyaltyAccount getLoyaltyAccount(Long userId);
}