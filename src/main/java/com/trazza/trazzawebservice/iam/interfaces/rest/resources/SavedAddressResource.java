package com.trazza.trazzawebservice.iam.interfaces.rest.resources;

public record SavedAddressResource(Long id, String label, String address, String district, double latitude,
                                   double longitude) {
}
