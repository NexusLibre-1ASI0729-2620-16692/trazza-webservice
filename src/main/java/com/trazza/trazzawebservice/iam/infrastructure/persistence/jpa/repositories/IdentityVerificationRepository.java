package com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {

    List<IdentityVerification> findByUserIdOrderBySubmittedAtDesc(Long userId);
}
