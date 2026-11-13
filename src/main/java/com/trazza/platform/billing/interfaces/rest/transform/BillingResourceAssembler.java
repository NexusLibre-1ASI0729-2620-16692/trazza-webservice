package com.trazza.platform.billing.interfaces.rest.transform;

import com.trazza.platform.billing.domain.model.aggregates.Plan;
import com.trazza.platform.billing.domain.services.CompensationView;
import com.trazza.platform.billing.domain.services.FreightSettlementView;
import com.trazza.platform.billing.domain.services.SubscriptionView;
import com.trazza.platform.billing.interfaces.rest.resources.BillingTripResource;
import com.trazza.platform.billing.interfaces.rest.resources.CancellationCompensationResource;
import com.trazza.platform.billing.interfaces.rest.resources.FreightSettlementResource;
import com.trazza.platform.billing.interfaces.rest.resources.PlanResource;
import com.trazza.platform.billing.interfaces.rest.resources.SubscribedVehicleResource;
import com.trazza.platform.billing.interfaces.rest.resources.SubscriptionPaymentResource;
import com.trazza.platform.billing.interfaces.rest.resources.SubscriptionResource;
import com.trazza.platform.execution.interfaces.acl.TripSnapshot;
import com.trazza.platform.iam.domain.model.valueobjects.Roles;

public final class BillingResourceAssembler {

    private BillingResourceAssembler() {
    }

    public static PlanResource toResource(Plan plan) {
        return new PlanResource(plan.getCode(), plan.getName(), plan.getMonitoringType().name(),
                plan.getPricingUnit().name(), plan.getPrice(), plan.getCurrency(), plan.getBillingPeriod().name(),
                plan.getMinVehicles(), plan.getMaxVehicles(), plan.includesIot());
    }

    public static SubscriptionResource toResource(SubscriptionView view) {
        var subscription = view.subscription();
        return new SubscriptionResource(subscription.getId(), toResource(view.plan()), subscription.getStatus().name(),
                subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd(),
                view.plan().priceFor(subscription.getVehicles().size()),
                view.vehicles().stream().map(v -> new SubscribedVehicleResource(v.id(), v.plate())).toList(),
                view.payments().stream().map(p -> new SubscriptionPaymentResource(p.getId(), p.getAmount(),
                        p.getCurrency(), p.getStatus().name(), p.getGatewayPaymentId(), p.getPaidAt())).toList(),
                subscription.getCancelledAt(), subscription.getCreatedAt());
    }

    public static FreightSettlementResource toResource(FreightSettlementView view) {
        var settlement = view.settlement();
        return new FreightSettlementResource(toResource(view.trip()), view.isCarrier() ? Roles.CARRIER : Roles.MERCHANT,
                settlement.getAmount(), settlement.getCurrency(), settlement.status().name(),
                settlement.getMerchantMarkedPaidAt(), settlement.getCarrierConfirmedAt(),
                settlement.getExternalReference());
    }

    public static CancellationCompensationResource toResource(CompensationView view) {
        var compensation = view.compensation();
        return new CancellationCompensationResource(toResource(view.trip()),
                view.isCarrier() ? Roles.CARRIER : Roles.MERCHANT, compensation.getAmount(), compensation.getCurrency(),
                compensation.getStatus().name(), compensation.getCreatedAt(), compensation.getSettledAt());
    }

    private static BillingTripResource toResource(TripSnapshot trip) {
        return new BillingTripResource(trip.publicId(), trip.status(), trip.originAddress(), trip.destinationAddress());
    }
}
