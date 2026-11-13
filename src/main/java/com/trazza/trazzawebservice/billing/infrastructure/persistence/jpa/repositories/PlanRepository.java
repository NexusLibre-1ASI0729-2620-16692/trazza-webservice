package com.trazza.trazzawebservice.billing.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.billing.domain.model.aggregates.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {

    List<Plan> findByActiveTrueOrderByPrice();

    Optional<Plan> findByCode(String code);
}
