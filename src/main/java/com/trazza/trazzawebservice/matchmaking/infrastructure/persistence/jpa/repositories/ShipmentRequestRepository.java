package com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ShipmentRequest;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.ShipmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRequestRepository extends JpaRepository<ShipmentRequest, Long> {

    Optional<ShipmentRequest> findByPublicId(UUID publicId);

    List<ShipmentRequest> findByMerchantProfileIdOrderByCreatedAtDesc(Long merchantProfileId);

    /** Open shipments whose pick-up window has not ended. */
    List<ShipmentRequest> findByStatusAndPickupUntilAfterOrderByPickupFrom(ShipmentRequestStatus status, Instant now);
}
