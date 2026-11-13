package com.trazza.platform.billing.domain.model.aggregates;

import com.trazza.platform.billing.domain.model.valueobjects.BillingPeriod;
import com.trazza.platform.billing.domain.model.valueobjects.MonitoringType;
import com.trazza.platform.billing.domain.model.valueobjects.PricingUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
@Getter
public class Plan {

    @Id
    private Integer id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitoringType monitoringType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingUnit pricingUnit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingPeriod billingPeriod;

    @Column(nullable = false)
    private int minVehicles;

    private Integer maxVehicles;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected Plan() {
    }

    public boolean includesIot() {
        return monitoringType == MonitoringType.IOT_CARGO || monitoringType == MonitoringType.FULL;
    }

    public boolean acceptsVehicleCount(int count) {
        return count >= minVehicles && (maxVehicles == null || count <= maxVehicles);
    }

    public BigDecimal priceFor(int vehicleCount) {
        return pricingUnit == PricingUnit.PER_VEHICLE ? price.multiply(BigDecimal.valueOf(vehicleCount)) : price;
    }
}
