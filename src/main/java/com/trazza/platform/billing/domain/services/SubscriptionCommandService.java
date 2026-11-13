package com.trazza.platform.billing.domain.services;

import com.trazza.platform.billing.domain.model.commands.CancelSubscriptionCommand;
import com.trazza.platform.billing.domain.model.commands.RenewSubscriptionCommand;
import com.trazza.platform.billing.domain.model.commands.SubscribeCommand;
import com.trazza.platform.shared.domain.model.Result;

public interface SubscriptionCommandService {

    Result<SubscriptionView> handle(SubscribeCommand command);

    Result<SubscriptionView> handle(RenewSubscriptionCommand command);

    Result<SubscriptionView> handle(CancelSubscriptionCommand command);
}
