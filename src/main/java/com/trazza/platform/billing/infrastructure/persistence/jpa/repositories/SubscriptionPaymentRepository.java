package com.trazza.platform.billing.infrastructure.persistence.jpa.repositories;

import com.trazza.platform.billing.domain.model.aggregates.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    List<SubscriptionPayment> findBySubscriptionIdInOrderByCreatedAtDescIdDesc(Collection<Long> subscriptionIds);
}
