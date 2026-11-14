package com.trazza.trazzawebservice.billing.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.billing.domain.model.aggregates.Subscription;
import com.trazza.trazzawebservice.billing.domain.model.valueobjects.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select distinct v.vehicleId from Subscription s join s.vehicles v "
            + "where s.status = :status and v.vehicleId in :vehicleIds")
    List<Long> findSubscribedVehicleIds(@Param("status") SubscriptionStatus status,
                                        @Param("vehicleIds") Collection<Long> vehicleIds);
}
