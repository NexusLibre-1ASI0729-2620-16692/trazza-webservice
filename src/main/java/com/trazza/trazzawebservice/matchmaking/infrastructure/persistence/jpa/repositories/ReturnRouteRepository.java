package com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ReturnRoute;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.ReturnRouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRouteRepository extends JpaRepository<ReturnRoute, Long> {

    Optional<ReturnRoute> findByPublicId(UUID publicId);

    List<ReturnRoute> findByCarrierProfileIdOrderByDepartureAtDesc(Long carrierProfileId);

    List<ReturnRoute> findByStatusAndDepartureAtBetween(ReturnRouteStatus status, Instant from, Instant until);
}
