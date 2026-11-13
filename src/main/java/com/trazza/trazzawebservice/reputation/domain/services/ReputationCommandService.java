package com.trazza.trazzawebservice.reputation.domain.services;

import com.trazza.trazzawebservice.reputation.domain.model.commands.RateTripCommand;
import com.trazza.trazzawebservice.reputation.domain.model.commands.RedeemPointsCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface ReputationCommandService {

    Result<RatingView> handle(RateTripCommand command);

    Result<LoyaltyAccount> handle(RedeemPointsCommand command);
}