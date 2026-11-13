package com.trazza.trazzawebservice.billing.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record SubscribeResource(String planCode, List<UUID> vehicleIds) {
}
