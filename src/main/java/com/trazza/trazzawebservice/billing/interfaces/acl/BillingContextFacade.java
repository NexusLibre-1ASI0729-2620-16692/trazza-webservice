package com.trazza.trazzawebservice.billing.interfaces.acl;

import java.math.BigDecimal;
import java.time.Instant;

public interface BillingContextFacade {

    void createFreightSettlement(Long tripId, BigDecimal amount, String currency);

    void createCancellationCompensation(Long tripId, BigDecimal amount, String currency, Instant now);
}
