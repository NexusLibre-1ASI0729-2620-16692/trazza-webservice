package com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.MerchantProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, Long> {

    Optional<MerchantProfile> findByUserId(Long userId);

    boolean existsByRuc(String ruc);
}
