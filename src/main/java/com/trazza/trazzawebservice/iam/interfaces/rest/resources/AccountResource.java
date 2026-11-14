package com.trazza.trazzawebservice.iam.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountResource(UUID id, String email, List<String> roles, IdentityResource identity,
                              CarrierProfileResource carrierProfile, MerchantProfileResource merchantProfile,
                              Instant createdAt) {
}
