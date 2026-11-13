package com.trazza.platform.billing.application.internal.queryservices;

import com.trazza.platform.billing.application.internal.SubscriptionViewBuilder;
import com.trazza.platform.billing.domain.model.aggregates.Plan;
import com.trazza.platform.billing.domain.services.BillingQueryService;
import com.trazza.platform.billing.domain.services.CompensationView;
import com.trazza.platform.billing.domain.services.FreightSettlementView;
import com.trazza.platform.billing.domain.services.SubscriptionView;
import com.trazza.platform.billing.infrastructure.persistence.jpa.repositories.CancellationCompensationRepository;
import com.trazza.platform.billing.infrastructure.persistence.jpa.repositories.FreightSettlementRepository;
import com.trazza.platform.billing.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.trazza.platform.billing.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import com.trazza.platform.execution.interfaces.acl.ExecutionContextFacade;
import com.trazza.platform.execution.interfaces.acl.TripSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BillingQueryServiceImpl implements BillingQueryService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final FreightSettlementRepository settlementRepository;
    private final CancellationCompensationRepository compensationRepository;
    private final ExecutionContextFacade executionContextFacade;
    private final SubscriptionViewBuilder viewBuilder;

    public BillingQueryServiceImpl(PlanRepository planRepository, SubscriptionRepository subscriptionRepository,
                                   FreightSettlementRepository settlementRepository,
                                   CancellationCompensationRepository compensationRepository,
                                   ExecutionContextFacade executionContextFacade, SubscriptionViewBuilder viewBuilder) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.settlementRepository = settlementRepository;
        this.compensationRepository = compensationRepository;
        this.executionContextFacade = executionContextFacade;
        this.viewBuilder = viewBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan> listPlans() {
        return planRepository.findByActiveTrueOrderByPrice();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionView> listMySubscriptions(Long userId) {
        return viewBuilder.build(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FreightSettlementView> listMySettlements(Long userId) {
        var trips = tripsOf(userId);
        if (trips.isEmpty()) return List.of();
        return settlementRepository.findByTripIdIn(trips.keySet()).stream()
                .map(settlement -> {
                    var trip = trips.get(settlement.getTripId());
                    return new FreightSettlementView(settlement, trip, Objects.equals(trip.carrierUserId(), userId));
                })
                .sorted(Comparator.comparing((FreightSettlementView view) -> view.trip().deliveredAt(),
                        Comparator.nullsLast(Comparator.<Instant>naturalOrder())).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompensationView> listMyCompensations(Long userId) {
        var trips = tripsOf(userId);
        if (trips.isEmpty()) return List.of();
        return compensationRepository.findByTripIdIn(trips.keySet()).stream()
                .map(compensation -> {
                    var trip = trips.get(compensation.getTripId());
                    return new CompensationView(compensation, trip, Objects.equals(trip.carrierUserId(), userId));
                })
                .sorted(Comparator.comparing((CompensationView view) -> view.compensation().getCreatedAt()).reversed())
                .toList();
    }

    private java.util.Map<Long, TripSnapshot> tripsOf(Long userId) {
        return executionContextFacade.listTripsOfUser(userId).stream()
                .collect(Collectors.toMap(TripSnapshot::id, Function.identity(), (first, second) -> first));
    }
}
