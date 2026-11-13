package com.trazza.trazzawebservice.billing.interfaces.rest.resources;

import java.util.UUID;

public record BillingTripResource(UUID id, String status, String originAddress, String destinationAddress) {
}
