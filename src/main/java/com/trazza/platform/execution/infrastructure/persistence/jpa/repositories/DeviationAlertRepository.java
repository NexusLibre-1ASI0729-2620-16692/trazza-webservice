package com.trazza.platform.execution.infrastructure.persistence.jpa.repositories;

import com.trazza.platform.execution.domain.model.aggregates.DeviationAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviationAlertRepository extends JpaRepository<DeviationAlert, Long> {

    List<DeviationAlert> findByTripIdOrderByDetectedAtDesc(Long tripId);

    Optional<DeviationAlert> findFirstByTripIdAndAcknowledgedAtIsNullOrderByDetectedAtDesc(Long tripId);

    Optional<DeviationAlert> findByIdAndTripId(Long id, Long tripId);

    long countByTripIdAndAcknowledgedAtIsNull(Long tripId);
}
