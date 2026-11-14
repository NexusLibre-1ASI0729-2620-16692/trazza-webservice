package com.trazza.trazzawebservice.iam.domain.model.commands;

import com.trazza.trazzawebservice.iam.domain.model.valueobjects.DriverSex;

public record DriverData(String fullName, DriverSex sex, Integer experienceStars, boolean active) {
}
