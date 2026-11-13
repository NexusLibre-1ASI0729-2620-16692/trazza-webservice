package com.trazza.platform.billing.domain.model.commands;

public record RenewSubscriptionCommand(Long userId, Long subscriptionId) {
}
