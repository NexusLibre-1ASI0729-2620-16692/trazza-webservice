package com.trazza.platform.billing.interfaces.rest.resources;

import java.math.BigDecimal;

public record PlanResource(String code, String name, String monitoringType, String pricingUnit, BigDecimal price,
                           String currency, String billingPeriod, int minVehicles, Integer maxVehicles,
                           boolean includesIotDevice) {
}
