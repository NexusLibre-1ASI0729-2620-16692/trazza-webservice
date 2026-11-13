package com.trazza.platform.billing.domain.services;

import com.trazza.platform.billing.domain.model.commands.ConfirmFreightReceivedCommand;
import com.trazza.platform.billing.domain.model.commands.MarkFreightPaidCommand;
import com.trazza.platform.billing.domain.model.commands.UpdateCompensationCommand;
import com.trazza.platform.shared.domain.model.Result;

public interface SettlementCommandService {

    Result<FreightSettlementView> handle(MarkFreightPaidCommand command);

    Result<FreightSettlementView> handle(ConfirmFreightReceivedCommand command);

    Result<CompensationView> handle(UpdateCompensationCommand command);
}
