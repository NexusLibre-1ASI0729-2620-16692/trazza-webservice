package com.trazza.trazzawebservice.billing.domain.model.aggregates;

import com.trazza.trazzawebservice.billing.domain.model.valueobjects.PaymentStatus;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.PublicIds;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "subscription_payments")
@Getter
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long subscriptionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(length = 100)
    private String gatewayPaymentId;

    private Instant paidAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected SubscriptionPayment() {
    }

    public static SubscriptionPayment simulated(Long subscriptionId, BigDecimal amount, String currency, Instant now) {
        var payment = new SubscriptionPayment();
        payment.subscriptionId = subscriptionId;
        payment.amount = amount;
        payment.currency = currency;
        payment.status = PaymentStatus.PAID;
        payment.gatewayPaymentId = "SIM-PAY-" + PublicIds.newCompactId();
        payment.paidAt = now;
        payment.createdAt = now;
        return payment;
    }
}
