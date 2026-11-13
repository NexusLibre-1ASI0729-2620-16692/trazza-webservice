package com.trazza.platform.billing.domain.model.commands;

public record CancelSubscriptionCommand(Long userId, Long subscriptionId) {
}
