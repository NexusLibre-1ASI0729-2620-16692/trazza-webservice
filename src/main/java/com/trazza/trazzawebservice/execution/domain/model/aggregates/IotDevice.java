package com.trazza.trazzawebservice.execution.domain.model.aggregates;

import com.trazza.trazzawebservice.execution.domain.model.valueobjects.IotDeviceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/** Cargo sensor that Trazza lends to a truck with an IoT monitoring plan ({@code iot_devices}). */
@Entity
@Table(name = "iot_devices")
@Getter
public class IotDevice {

    @Id
    private Long id;

    @Column(nullable = false, length = 60)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IotDeviceStatus status;

    private Long assignedVehicleId;
    private Instant loanedAt;

    @Column(nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected IotDevice() {
    }

    public void loanTo(Long vehicleId, Instant now) {
        status = IotDeviceStatus.LOANED;
        assignedVehicleId = vehicleId;
        loanedAt = now;
    }

    public void returnDevice() {
        status = IotDeviceStatus.AVAILABLE;
        assignedVehicleId = null;
        loanedAt = null;
    }
}
