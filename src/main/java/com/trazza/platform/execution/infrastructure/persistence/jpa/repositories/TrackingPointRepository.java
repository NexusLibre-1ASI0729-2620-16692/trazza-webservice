package com.trazza.platform.execution.infrastructure.persistence.jpa.repositories;

import com.trazza.platform.execution.domain.model.aggregates.TrackingPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackingPointRepository extends JpaRepository<TrackingPoint, Long> {

    List<TrackingPoint> findByTripIdOrderByRecordedAtAscIdAsc(Long tripId);

    Optional<TrackingPoint> findFirstByTripIdOrderByRecordedAtDescIdDesc(Long tripId);
}
