package com.trazza.trazzawebservice.reputation.application.internal.queryservices;

import com.trazza.trazzawebservice.iam.interfaces.acl.ContactSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.reputation.application.internal.RatingViewBuilder;
import com.trazza.trazzawebservice.reputation.domain.model.aggregates.Rating;
import com.trazza.trazzawebservice.reputation.domain.services.LoyaltyAccount;
import com.trazza.trazzawebservice.reputation.domain.services.RatingView;
import com.trazza.trazzawebservice.reputation.domain.services.ReputationQueryService;
import com.trazza.trazzawebservice.reputation.domain.services.ReputationSummary;
import com.trazza.trazzawebservice.reputation.infrastructure.persistence.jpa.repositories.LoyaltyPointEntryRepository;
import com.trazza.trazzawebservice.reputation.infrastructure.persistence.jpa.repositories.RatingRepository;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ReputationQueryServiceImpl implements ReputationQueryService {

    private final RatingRepository ratingRepository;
    private final LoyaltyPointEntryRepository loyaltyRepository;
    private final IamContextFacade iamContextFacade;
    private final RatingViewBuilder viewBuilder;

    public ReputationQueryServiceImpl(RatingRepository ratingRepository, LoyaltyPointEntryRepository loyaltyRepository,
                                      IamContextFacade iamContextFacade, RatingViewBuilder viewBuilder) {
        this.ratingRepository = ratingRepository;
        this.loyaltyRepository = loyaltyRepository;
        this.iamContextFacade = iamContextFacade;
        this.viewBuilder = viewBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingView> listReceived(Long userId) {
        return viewBuilder.build(ratingRepository.findByRatedUserIdOrderByCreatedAtDesc(userId), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingView> listGiven(Long userId) {
        return viewBuilder.build(ratingRepository.findByRaterUserIdOrderByCreatedAtDesc(userId), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ReputationSummary> getSummary(UUID userPublicId) {
        var userId = iamContextFacade.findUserIdByPublicId(userPublicId).orElse(null);
        if (userId == null) return Result.failure(CommandError.notFound("user-not-found"));
        var name = iamContextFacade.findContact(userId).map(ContactSnapshot::displayName).orElse("");
        var ratings = ratingRepository.findByRatedUserIdOrderByCreatedAtDesc(userId);
        Double average = ratings.isEmpty() ? null
                : BigDecimal.valueOf(ratings.stream().mapToInt(Rating::getScore).average().orElse(0))
                .setScale(2, RoundingMode.HALF_EVEN).doubleValue();
        return Result.success(new ReputationSummary(userPublicId, name, average, ratings.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public LoyaltyAccount getLoyaltyAccount(Long userId) {
        return new LoyaltyAccount((int) loyaltyRepository.sumPoints(userId),
                loyaltyRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId));
    }
}