package com.trazza.platform.billing.application.internal;

import com.trazza.platform.billing.domain.model.aggregates.Plan;
import com.trazza.platform.billing.domain.model.aggregates.Subscription;
import com.trazza.platform.billing.domain.services.SubscribedVehicle;
import com.trazza.platform.billing.domain.services.SubscriptionView;
import com.trazza.platform.billing.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.trazza.platform.billing.infrastructure.persistence.jpa.repositories.SubscriptionPaymentRepository;
import com.trazza.platform.iam.interfaces.acl.IamContextFacade;
import com.trazza.platform.iam.interfaces.acl.VehicleSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SubscriptionViewBuilder {

    private final PlanRepository planRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final IamContextFacade iamContextFacade;

    public SubscriptionViewBuilder(PlanRepository planRepository, SubscriptionPaymentRepository paymentRepository,
                                   IamContextFacade iamContextFacade) {
        this.planRepository = planRepository;
        this.paymentRepository = paymentRepository;
        this.iamContextFacade = iamContextFacade;
    }

    public List<SubscriptionView> build(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) return List.of();
        var plans = planRepository.findAll().stream().collect(Collectors.toMap(Plan::getId, Function.identity()));
        var payments = paymentRepository.findBySubscriptionIdInOrderByCreatedAtDescIdDesc(
                subscriptions.stream().map(Subscription::getId).toList());
        var vehicles = iamContextFacade.listVehicles(subscriptions.stream()
                        .flatMap(subscription -> subscription.vehicleIds().stream()).distinct().toList())
                .stream().collect(Collectors.toMap(VehicleSnapshot::id, Function.identity()));

        return subscriptions.stream().map(subscription -> new SubscriptionView(subscription,
                plans.get(subscription.getPlanId()),
                subscription.vehicleIds().stream().map(vehicles::get).filter(Objects::nonNull)
                        .map(vehicle -> new SubscribedVehicle(vehicle.publicId(), vehicle.plate())).toList(),
                payments.stream().filter(payment -> Objects.equals(payment.getSubscriptionId(), subscription.getId()))
                        .toList())).toList();
    }

    public SubscriptionView build(Subscription subscription) {
        return build(List.of(subscription)).getFirst();
    }
}
