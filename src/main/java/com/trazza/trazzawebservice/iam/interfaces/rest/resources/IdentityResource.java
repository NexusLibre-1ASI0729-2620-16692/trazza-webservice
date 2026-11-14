package com.trazza.trazzawebservice.iam.interfaces.rest.resources;

public record IdentityResource(String fullName, String dniMasked, String phone, String kycStatus) {
}
