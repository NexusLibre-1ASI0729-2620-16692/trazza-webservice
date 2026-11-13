package com.trazza.trazzawebservice.reputation.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.reputation.domain.model.aggregates.LoyaltyPointEntry;
import com.trazza.trazzawebservice.reputation.domain.model.valueobjects.LoyaltyReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyPointEntryRepository extends JpaRepository<LoyaltyPointEntry, Long> {

    List<LoyaltyPointEntry> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @Query("select coalesce(sum(e.points), 0) from LoyaltyPointEntry e where e.userId = :userId")
    long sumPoints(@Param("userId") Long userId);

    boolean existsByUserIdAndTripIdAndReason(Long userId, Long tripId, LoyaltyReason reason);
}