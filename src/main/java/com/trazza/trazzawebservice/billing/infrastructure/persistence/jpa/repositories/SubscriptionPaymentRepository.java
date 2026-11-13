package com.trazza.trazzawebservice.billing.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.billing.domain.model.aggregates.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    List<SubscriptionPayment> findBySubscriptionIdInOrderByCreatedAtDescIdDesc(Collection<Long> subscriptionIds);
}
