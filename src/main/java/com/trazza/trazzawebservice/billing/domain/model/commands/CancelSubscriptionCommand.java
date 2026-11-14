package com.trazza.trazzawebservice.billing.domain.model.commands;

public record CancelSubscriptionCommand(Long userId, Long subscriptionId) {
}
