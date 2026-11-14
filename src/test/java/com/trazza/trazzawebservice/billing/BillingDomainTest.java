package com.trazza.trazzawebservice.billing;

import com.trazza.trazzawebservice.billing.domain.model.aggregates.CancellationCompensation;
import com.trazza.trazzawebservice.billing.domain.model.aggregates.FreightSettlement;
import com.trazza.trazzawebservice.billing.domain.model.aggregates.Plan;
import com.trazza.trazzawebservice.billing.domain.model.aggregates.Subscription;
import com.trazza.trazzawebservice.billing.domain.model.valueobjects.CompensationStatus;
import com.trazza.trazzawebservice.billing.domain.model.valueobjects.MonitoringType;
import com.trazza.trazzawebservice.billing.domain.model.valueobjects.PricingUnit;
import com.trazza.trazzawebservice.billing.domain.model.valueobjects.SettlementStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingDomainTest {

    private static Plan newPlan(MonitoringType type, PricingUnit unit, String price, int min, Integer max) throws Exception {
        var constructor = Plan.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var plan = constructor.newInstance();
        ReflectionTestUtils.setField(plan, "monitoringType", type);
        ReflectionTestUtils.setField(plan, "pricingUnit", unit);
        ReflectionTestUtils.setField(plan, "price", new BigDecimal(price));
        ReflectionTestUtils.setField(plan, "minVehicles", min);
        ReflectionTestUtils.setField(plan, "maxVehicles", max);
        return plan;
    }

    @Test
    void perVehiclePlanMultipliesThePrice() throws Exception {
        var plan = newPlan(MonitoringType.ROUTE, PricingUnit.PER_VEHICLE, "10.00", 1, null);
        assertEquals(new BigDecimal("30.00"), plan.priceFor(3));
        assertFalse(plan.includesIot());
    }

    @Test
    void packagePlanHasAFixedPriceAndAVehicleRange() throws Exception {
        var plan = newPlan(MonitoringType.FULL, PricingUnit.PACKAGE, "200.00", 5, 6);
        assertEquals(new BigDecimal("200.00"), plan.priceFor(6));
        assertTrue(plan.includesIot());
        assertFalse(plan.acceptsVehicleCount(4));
        assertTrue(plan.acceptsVehicleCount(5));
        assertFalse(plan.acceptsVehicleCount(7));
    }

    @Test
    void renewalMovesToTheNextCalendarMonth() {
        var start = Instant.parse("2026-01-31T10:00:00Z");
        var subscription = new Subscription(1L, 1, List.of(7L, 7L, 8L), "SIM-SUB-1", start);
        assertEquals(2, subscription.vehicleIds().size());
        assertEquals(Instant.parse("2026-02-28T10:00:00Z"), subscription.getCurrentPeriodEnd());
        subscription.renew();
        assertEquals(Instant.parse("2026-02-28T10:00:00Z"), subscription.getCurrentPeriodStart());
        assertEquals(Instant.parse("2026-03-28T10:00:00Z"), subscription.getCurrentPeriodEnd());
    }

    @Test
    void freightSettlementGoesFromPendingToSettled() {
        var settlement = new FreightSettlement(1L, new BigDecimal("120.00"), "PEN");
        assertEquals(SettlementStatus.PENDING, settlement.status());
        settlement.markPaid("  YAPE-1  ", Instant.now());
        assertEquals(SettlementStatus.MARKED_PAID, settlement.status());
        assertEquals("YAPE-1", settlement.getExternalReference());
        settlement.confirmReceived(Instant.now());
        assertEquals(SettlementStatus.SETTLED, settlement.status());
    }

    @Test
    void disputedCompensationCanStillBeWaivedButNotTwice() {
        var compensation = new CancellationCompensation(1L, new BigDecimal("30.00"), "PEN", Instant.now());
        compensation.dispute();
        assertEquals(CompensationStatus.DISPUTED, compensation.getStatus());
        compensation.waive(Instant.now());
        assertEquals(CompensationStatus.WAIVED, compensation.getStatus());
        assertFalse(compensation.isOpen());
        compensation.confirmPaid(Instant.now());
        assertEquals(CompensationStatus.WAIVED, compensation.getStatus());
    }
}
