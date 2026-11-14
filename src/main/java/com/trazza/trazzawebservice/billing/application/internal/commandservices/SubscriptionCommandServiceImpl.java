package com.trazza.trazzawebservice.billing.application.internal.commandservices;

import com.trazza.trazzawebservice.billing.application.internal.SubscriptionViewBuilder;
import com.trazza.trazzawebservice.billing.domain.model.aggregates.Subscription;
import com.trazza.trazzawebservice.billing.domain.model.aggregates.SubscriptionPayment;
import com.trazza.trazzawebservice.billing.domain.model.commands.CancelSubscriptionCommand;
import com.trazza.trazzawebservice.billing.domain.model.commands.RenewSubscriptionCommand;
import com.trazza.trazzawebservice.billing.domain.model.commands.SubscribeCommand;
import com.trazza.trazzawebservice.billing.domain.model.valueobjects.SubscriptionStatus;
import com.trazza.trazzawebservice.billing.domain.services.SubscriptionCommandService;
import com.trazza.trazzawebservice.billing.domain.services.SubscriptionView;
import com.trazza.trazzawebservice.billing.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.trazza.trazzawebservice.billing.infrastructure.persistence.jpa.repositories.SubscriptionPaymentRepository;
import com.trazza.trazzawebservice.billing.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import com.trazza.trazzawebservice.execution.interfaces.acl.ExecutionContextFacade;
import com.trazza.trazzawebservice.iam.interfaces.acl.IamContextFacade;
import com.trazza.trazzawebservice.iam.interfaces.acl.VehicleSnapshot;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.PublicIds;
import com.trazza.trazzawebservice.shared.infrastructure.transactions.Transactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final IamContextFacade iamContextFacade;
    private final ExecutionContextFacade executionContextFacade;
    private final SubscriptionViewBuilder viewBuilder;

    public SubscriptionCommandServiceImpl(PlanRepository planRepository, SubscriptionRepository subscriptionRepository,
                                          SubscriptionPaymentRepository paymentRepository,
                                          IamContextFacade iamContextFacade,
                                          ExecutionContextFacade executionContextFacade,
                                          SubscriptionViewBuilder viewBuilder) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.iamContextFacade = iamContextFacade;
        this.executionContextFacade = executionContextFacade;
        this.viewBuilder = viewBuilder;
    }

    @Override
    @Transactional
    public Result<SubscriptionView> handle(SubscribeCommand command) {
        return Transactions.rollbackOnFailure(subscribe(command));
    }

    @Override
    @Transactional
    public Result<SubscriptionView> handle(RenewSubscriptionCommand command) {
        var subscription = findOwned(command.userId(), command.subscriptionId());
        if (subscription == null) return Result.failure(CommandError.notFound("subscription-not-found"));
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED)
            return Result.failure(CommandError.conflict("subscription-cancelled"));

        var plan = planRepository.findById(subscription.getPlanId()).orElseThrow();
        paymentRepository.save(SubscriptionPayment.simulated(subscription.getId(),
                plan.priceFor(subscription.getVehicles().size()), plan.getCurrency(), Instant.now()));
        subscription.renew();
        subscriptionRepository.saveAndFlush(subscription);
        return Result.success(viewBuilder.build(subscription));
    }

    @Override
    @Transactional
    public Result<SubscriptionView> handle(CancelSubscriptionCommand command) {
        var subscription = findOwned(command.userId(), command.subscriptionId());
        if (subscription == null) return Result.failure(CommandError.notFound("subscription-not-found"));
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED)
            return Result.failure(CommandError.conflict("subscription-cancelled"));

        var plan = planRepository.findById(subscription.getPlanId()).orElseThrow();
        subscription.cancel(Instant.now());
        if (plan.includesIot()) executionContextFacade.returnIotDevices(subscription.vehicleIds());
        subscriptionRepository.saveAndFlush(subscription);
        return Result.success(viewBuilder.build(subscription));
    }

    private Result<SubscriptionView> subscribe(SubscribeCommand command) {
        var carrierProfileId = iamContextFacade.findCarrierProfileId(command.userId());
        if (carrierProfileId.isEmpty()) return Result.failure(CommandError.forbidden("carrier-profile-required"));

        var code = command.planCode() == null ? "" : command.planCode().trim().toUpperCase(Locale.ROOT);
        var plan = planRepository.findByCode(code).orElse(null);
        if (plan == null || !plan.isActive()) return Result.failure(CommandError.notFound("plan-not-found"));

        var requested = List.copyOf(new LinkedHashSet<>(command.vehicleIds() == null ? List.of()
                : command.vehicleIds().stream().filter(Objects::nonNull).toList()));
        if (!plan.acceptsVehicleCount(requested.size()))
            return Result.failure(CommandError.validation("plan-vehicle-count-invalid"));

        var vehicles = iamContextFacade.listVehiclesByPublicIds(requested);
        if (vehicles.size() != requested.size()
                || vehicles.stream().anyMatch(v -> !Objects.equals(v.carrierProfileId(), carrierProfileId.get())))
            return Result.failure(CommandError.notFound("vehicle-not-found"));
        if (vehicles.stream().anyMatch(v -> !v.active())) return Result.failure(CommandError.conflict("vehicle-inactive"));

        var vehicleIds = vehicles.stream().map(VehicleSnapshot::id).toList();
        if (!subscriptionRepository.findSubscribedVehicleIds(SubscriptionStatus.ACTIVE, vehicleIds).isEmpty())
            return Result.failure(CommandError.conflict("vehicle-already-subscribed"));

        var now = Instant.now();
        if (plan.includesIot() && !executionContextFacade.loanIotDevices(vehicleIds, now))
            return Result.failure(CommandError.conflict("iot-device-unavailable"));

        var subscription = subscriptionRepository.saveAndFlush(new Subscription(command.userId(), plan.getId(),
                vehicleIds, "SIM-SUB-" + PublicIds.newCompactId(), now));
        paymentRepository.saveAndFlush(SubscriptionPayment.simulated(subscription.getId(),
                plan.priceFor(vehicleIds.size()), plan.getCurrency(), now));
        return Result.success(viewBuilder.build(subscription));
    }

    private Subscription findOwned(Long userId, Long subscriptionId) {
        if (subscriptionId == null) return null;
        return subscriptionRepository.findById(subscriptionId)
                .filter(subscription -> Objects.equals(subscription.getUserId(), userId))
                .orElse(null);
    }
}
