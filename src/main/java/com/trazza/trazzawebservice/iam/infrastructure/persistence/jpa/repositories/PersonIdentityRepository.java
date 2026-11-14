package com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.PersonIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonIdentityRepository extends JpaRepository<PersonIdentity, Long> {

    boolean existsByDniHash(byte[] dniHash);
}
