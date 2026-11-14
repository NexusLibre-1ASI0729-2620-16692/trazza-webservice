package com.trazza.trazzawebservice.reputation.interfaces.rest.transform;

import com.trazza.trazzawebservice.reputation.domain.services.LoyaltyAccount;
import com.trazza.trazzawebservice.reputation.domain.services.RatingView;
import com.trazza.trazzawebservice.reputation.domain.services.ReputationSummary;
import com.trazza.trazzawebservice.reputation.interfaces.rest.resources.LoyaltyAccountResource;
import com.trazza.trazzawebservice.reputation.interfaces.rest.resources.LoyaltyEntryResource;
import com.trazza.trazzawebservice.reputation.interfaces.rest.resources.RatingResource;
import com.trazza.trazzawebservice.reputation.interfaces.rest.resources.ReputationSummaryResource;

public final class ReputationResourceAssembler {

    private ReputationResourceAssembler() {
    }

    public static RatingResource toResource(RatingView view) {
        var rating = view.rating();
        return new RatingResource(rating.getId(), view.tripPublicId(), view.counterpartPublicId(),
                view.counterpartName(), rating.getScore(), rating.getComment(), rating.getCreatedAt());
    }

    public static ReputationSummaryResource toResource(ReputationSummary summary) {
        return new ReputationSummaryResource(summary.userPublicId(), summary.name(), summary.averageScore(),
                summary.ratingsCount());
    }

    public static LoyaltyAccountResource toResource(LoyaltyAccount account) {
        return new LoyaltyAccountResource(account.balance(), account.entries().stream()
                .map(entry -> new LoyaltyEntryResource(entry.getId(), entry.getPoints(), entry.getReason().name(),
                        entry.getCreatedAt()))
                .toList());
    }
}