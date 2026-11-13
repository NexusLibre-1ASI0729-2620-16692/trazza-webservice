package com.trazza.trazzawebservice.billing.domain.model.commands;

public record RenewSubscriptionCommand(Long userId, Long subscriptionId) {
}
