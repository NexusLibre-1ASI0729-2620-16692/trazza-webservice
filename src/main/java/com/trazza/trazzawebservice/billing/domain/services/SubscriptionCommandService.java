package com.trazza.trazzawebservice.billing.domain.services;

import com.trazza.trazzawebservice.billing.domain.model.commands.CancelSubscriptionCommand;
import com.trazza.trazzawebservice.billing.domain.model.commands.RenewSubscriptionCommand;
import com.trazza.trazzawebservice.billing.domain.model.commands.SubscribeCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface SubscriptionCommandService {

    Result<SubscriptionView> handle(SubscribeCommand command);

    Result<SubscriptionView> handle(RenewSubscriptionCommand command);

    Result<SubscriptionView> handle(CancelSubscriptionCommand command);
}
