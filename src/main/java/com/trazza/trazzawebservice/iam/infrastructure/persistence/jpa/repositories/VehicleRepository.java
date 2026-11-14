package com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByCarrierProfileIdOrderByPlate(Long carrierProfileId);

    Optional<Vehicle> findByPublicId(UUID publicId);

    boolean existsByPlate(String plate);

    boolean existsByPlateAndIdNot(String plate, Long id);
}
