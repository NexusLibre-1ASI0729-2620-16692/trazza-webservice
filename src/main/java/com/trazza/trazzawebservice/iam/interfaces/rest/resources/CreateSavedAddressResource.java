package com.trazza.trazzawebservice.iam.interfaces.rest.resources;

public record CreateSavedAddressResource(String label, String address, String district, Double latitude,
                                         Double longitude) {
}
