package com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPublicId(UUID publicId);

    boolean existsByEmail(String email);
}
