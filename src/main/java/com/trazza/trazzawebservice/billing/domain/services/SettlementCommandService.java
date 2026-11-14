package com.trazza.trazzawebservice.billing.domain.services;

import com.trazza.trazzawebservice.billing.domain.model.commands.ConfirmFreightReceivedCommand;
import com.trazza.trazzawebservice.billing.domain.model.commands.MarkFreightPaidCommand;
import com.trazza.trazzawebservice.billing.domain.model.commands.UpdateCompensationCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface SettlementCommandService {

    Result<FreightSettlementView> handle(MarkFreightPaidCommand command);

    Result<FreightSettlementView> handle(ConfirmFreightReceivedCommand command);

    Result<CompensationView> handle(UpdateCompensationCommand command);
}
