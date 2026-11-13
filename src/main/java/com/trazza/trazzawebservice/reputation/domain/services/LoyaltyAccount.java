package com.trazza.trazzawebservice.reputation.domain.services;

import com.trazza.trazzawebservice.reputation.domain.model.aggregates.LoyaltyPointEntry;

import java.util.List;

public record LoyaltyAccount(int balance, List<LoyaltyPointEntry> entries) {
}