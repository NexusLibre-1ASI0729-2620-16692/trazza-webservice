package com.trazza.trazzawebservice.reputation.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.reputation.domain.model.aggregates.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByTripIdAndRaterUserId(Long tripId, Long raterUserId);

    List<Rating> findByRatedUserIdOrderByCreatedAtDesc(Long ratedUserId);

    List<Rating> findByRaterUserIdOrderByCreatedAtDesc(Long raterUserId);
}