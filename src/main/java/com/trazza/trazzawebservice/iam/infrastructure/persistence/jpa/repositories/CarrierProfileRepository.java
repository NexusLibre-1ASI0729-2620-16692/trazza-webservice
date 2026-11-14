package com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.CarrierProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarrierProfileRepository extends JpaRepository<CarrierProfile, Long> {

    Optional<CarrierProfile> findByUserId(Long userId);
}
