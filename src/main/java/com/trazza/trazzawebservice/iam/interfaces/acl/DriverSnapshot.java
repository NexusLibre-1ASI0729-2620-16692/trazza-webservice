package com.trazza.trazzawebservice.iam.interfaces.acl;

public record DriverSnapshot(Long id, Long carrierProfileId, String fullName, int experienceStars, boolean active) {
}
