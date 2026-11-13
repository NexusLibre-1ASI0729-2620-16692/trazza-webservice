package com.trazza.platform.execution.infrastructure.persistence.jpa.repositories;

import com.trazza.platform.execution.domain.model.aggregates.IotDevice;
import com.trazza.platform.execution.domain.model.valueobjects.IotDeviceStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface IotDeviceRepository extends JpaRepository<IotDevice, Long> {

    List<IotDevice> findByStatusAndAssignedVehicleIdIsNullOrderById(IotDeviceStatus status, Limit limit);

    List<IotDevice> findByAssignedVehicleIdIn(Collection<Long> vehicleIds);
}
