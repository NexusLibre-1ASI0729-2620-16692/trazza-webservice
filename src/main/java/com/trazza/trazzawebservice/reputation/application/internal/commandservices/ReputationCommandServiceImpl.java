package com.trazza.trazzawebservice.reputation.application.internal.commandservices;

import com.trazza.trazzawebservice.execution.interfaces.acl.ExecutionContextFacade;
import com.trazza.trazzawebservice.reputation.application.internal.RatingViewBuilder;
import com.trazza.trazzawebservice.reputation.domain.model.aggregates.LoyaltyPointEntry;
import com.trazza.trazzawebservice.reputation.domain.model.aggregates.Rating;
import com.trazza.trazzawebservice.reputation.domain.model.commands.RateTripCommand;
import com.trazza.trazzawebservice.reputation.domain.model.commands.RedeemPointsCommand;
import com.trazza.trazzawebservice.reputation.domain.model.valueobjects.LoyaltyReason;
import com.trazza.trazzawebservice.reputation.domain.services.LoyaltyAccount;
import com.trazza.trazzawebservice.reputation.domain.services.RatingView;
import com.trazza.trazzawebservice.reputation.domain.services.ReputationCommandService;
import com.trazza.trazzawebservice.reputation.infrastructure.persistence.jpa.repositories.LoyaltyPointEntryRepository;
import com.trazza.trazzawebservice.reputation.infrastructure.persistence.jpa.repositories.RatingRepository;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import com.trazza.trazzawebservice.shared.infrastructure.transactions.Transactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class ReputationCommandServiceImpl implements ReputationCommandService {

    private final RatingRepository ratingRepository;
    private final LoyaltyPointEntryRepository loyaltyRepository;
    private final ExecutionContextFacade executionContextFacade;
    private final RatingViewBuilder viewBuilder;

    public ReputationCommandServiceImpl(RatingRepository ratingRepository, LoyaltyPointEntryRepository loyaltyRepository,
                                        ExecutionContextFacade executionContextFacade, RatingViewBuilder viewBuilder) {
        this.ratingRepository = ratingRepository;
        this.loyaltyRepository = loyaltyRepository;
        this.executionContextFacade = executionContextFacade;
        this.viewBuilder = viewBuilder;
    }

    @Override
    @Transactional
    public Result<RatingView> handle(RateTripCommand command) {
        var trip = executionContextFacade.findTrip(command.tripId())
                .filter(t -> Objects.equals(t.carrierUserId(), command.userId())
                        || Objects.equals(t.merchantUserId(), command.userId()))
                .orElse(null);
        if (trip == null) return Result.failure(CommandError.notFound("trip-not-found"));
        if (!"CONFIRMED".equals(trip.status())) return Result.failure(CommandError.conflict("trip-not-confirmed"));
        if (command.score() == null || command.score() < 1 || command.score() > 5)
            return Result.failure(CommandError.validation("score-invalid"));
        if (command.comment() != null && command.comment().trim().length() > Rating.MAX_COMMENT_LENGTH)
            return Result.failure(CommandError.validation("comment-invalid"));
        if (ratingRepository.existsByTripIdAndRaterUserId(trip.id(), command.userId()))
            return Result.failure(CommandError.conflict("rating-already-given"));

        var now = Instant.now();
        var ratedUserId = Objects.equals(command.userId(), trip.carrierUserId()) ? trip.merchantUserId()
                : trip.carrierUserId();
        var rating = ratingRepository.saveAndFlush(new Rating(trip.id(), command.userId(), ratedUserId, command.score(),
                command.comment(), now));
        loyaltyRepository.saveAndFlush(new LoyaltyPointEntry(command.userId(), trip.id(),
                LoyaltyPointEntry.RATING_GIVEN_POINTS, LoyaltyReason.RATING_GIVEN, now));
        return Result.success(viewBuilder.build(List.of(rating), command.userId()).getFirst());
    }

    @Override
    @Transactional
    public Result<LoyaltyAccount> handle(RedeemPointsCommand command) {
        return Transactions.rollbackOnFailure(redeem(command));
    }

    private Result<LoyaltyAccount> redeem(RedeemPointsCommand command) {
        if (command.points() == null || command.points() <= 0)
            return Result.failure(CommandError.validation("points-invalid"));
        var balance = loyaltyRepository.sumPoints(command.userId());
        if (command.points() > balance) return Result.failure(CommandError.conflict("points-insufficient"));

        loyaltyRepository.saveAndFlush(new LoyaltyPointEntry(command.userId(), null, -command.points(),
                LoyaltyReason.REDEMPTION, Instant.now()));
        return Result.success(new LoyaltyAccount((int) loyaltyRepository.sumPoints(command.userId()),
                loyaltyRepository.findByUserIdOrderByCreatedAtDescIdDesc(command.userId())));
    }
}