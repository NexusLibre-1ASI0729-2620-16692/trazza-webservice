package com.trazza.trazzawebservice.reputation.interfaces.rest.resources;

import java.util.List;

public record LoyaltyAccountResource(int balance, List<LoyaltyEntryResource> entries) {
}