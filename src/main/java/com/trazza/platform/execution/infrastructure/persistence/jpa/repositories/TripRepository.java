package com.trazza.platform.execution.infrastructure.persistence.jpa.repositories;

import com.trazza.platform.execution.domain.model.aggregates.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByPublicId(UUID publicId);

    Optional<Trip> findByMatchProposalId(Long matchProposalId);

    List<Trip> findByMatchProposalIdIn(Collection<Long> matchProposalIds);
}
