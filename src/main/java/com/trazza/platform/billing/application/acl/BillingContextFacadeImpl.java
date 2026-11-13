package com.trazza.platform.billing.application.acl;

import com.trazza.platform.billing.domain.model.aggregates.CancellationCompensation;
import com.trazza.platform.billing.domain.model.aggregates.FreightSettlement;
import com.trazza.platform.billing.infrastructure.persistence.jpa.repositories.CancellationCompensationRepository;
import com.trazza.platform.billing.infrastructure.persistence.jpa.repositories.FreightSettlementRepository;
import com.trazza.platform.billing.interfaces.acl.BillingContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class BillingContextFacadeImpl implements BillingContextFacade {

    private final FreightSettlementRepository settlementRepository;
    private final CancellationCompensationRepository compensationRepository;

    public BillingContextFacadeImpl(FreightSettlementRepository settlementRepository,
                                    CancellationCompensationRepository compensationRepository) {
        this.settlementRepository = settlementRepository;
        this.compensationRepository = compensationRepository;
    }

    @Override
    @Transactional
    public void createFreightSettlement(Long tripId, BigDecimal amount, String currency) {
        if (settlementRepository.existsById(tripId)) return;
        settlementRepository.save(new FreightSettlement(tripId, amount, currency));
    }

    @Override
    @Transactional
    public void createCancellationCompensation(Long tripId, BigDecimal amount, String currency, Instant now) {
        if (amount.signum() <= 0 || compensationRepository.existsById(tripId)) return;
        compensationRepository.save(new CancellationCompensation(tripId, amount, currency, now));
    }
}
