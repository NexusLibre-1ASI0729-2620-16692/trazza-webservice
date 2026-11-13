package com.trazza.trazzawebservice.execution.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.execution.domain.model.aggregates.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByTripIdOrderByReportedAtDesc(Long tripId);

    Optional<Incident> findByIdAndTripId(Long id, Long tripId);
}
