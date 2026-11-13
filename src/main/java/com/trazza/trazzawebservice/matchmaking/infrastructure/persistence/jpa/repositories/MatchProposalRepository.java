package com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.MatchProposal;
import com.trazza.trazzawebservice.matchmaking.domain.model.valueobjects.MatchProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchProposalRepository extends JpaRepository<MatchProposal, Long> {

    Optional<MatchProposal> findByPublicId(UUID publicId);

    boolean existsByShipmentRequestIdAndReturnRouteId(Long shipmentRequestId, Long returnRouteId);

    List<MatchProposal> findByReturnRouteIdInOrderByCreatedAtDesc(Collection<Long> returnRouteIds);

    List<MatchProposal> findByShipmentRequestIdInOrderByCreatedAtDesc(Collection<Long> shipmentRequestIds);

    List<MatchProposal> findByReturnRouteIdAndStatus(Long returnRouteId, MatchProposalStatus status);
}
