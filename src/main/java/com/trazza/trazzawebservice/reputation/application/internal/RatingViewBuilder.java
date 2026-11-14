package com.trazza.trazzawebservice.reputation.application.internal;

import com.trazza.trazzawebservice.execution.interfaces.acl.ExecutionContextFacade;
import com.trazza.trazzawebservice.execution.interfaces.acl.TripSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.ContactSnapshot;
import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.reputation.domain.model.aggregates.Rating;
import com.trazza.trazzawebservice.reputation.domain.services.RatingView;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RatingViewBuilder {

    private final ExecutionContextFacade executionContextFacade;
    private final IamContextFacade iamContextFacade;

    public RatingViewBuilder(ExecutionContextFacade executionContextFacade, IamContextFacade iamContextFacade) {
        this.executionContextFacade = executionContextFacade;
        this.iamContextFacade = iamContextFacade;
    }

    public List<RatingView> build(List<Rating> ratings, Long userId) {
        if (ratings.isEmpty()) return List.of();
        var trips = executionContextFacade.listTrips(ratings.stream().map(Rating::getTripId).distinct().toList())
                .stream().collect(Collectors.toMap(TripSnapshot::id, Function.identity()));
        var contacts = new HashMap<Long, Optional<ContactSnapshot>>();
        return ratings.stream().map(rating -> {
            var counterpartId = Objects.equals(rating.getRaterUserId(), userId) ? rating.getRatedUserId()
                    : rating.getRaterUserId();
            var contact = contacts.computeIfAbsent(counterpartId, iamContextFacade::findContact).orElse(null);
            var trip = trips.get(rating.getTripId());
            return new RatingView(rating, trip == null ? null : trip.publicId(),
                    contact == null ? null : contact.publicId(), contact == null ? "" : contact.displayName());
        }).toList();
    }
}