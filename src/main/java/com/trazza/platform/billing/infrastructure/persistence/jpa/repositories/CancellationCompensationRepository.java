package com.trazza.platform.billing.infrastructure.persistence.jpa.repositories;

import com.trazza.platform.billing.domain.model.aggregates.CancellationCompensation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CancellationCompensationRepository extends JpaRepository<CancellationCompensation, Long> {

    List<CancellationCompensation> findByTripIdIn(Collection<Long> tripIds);
}
