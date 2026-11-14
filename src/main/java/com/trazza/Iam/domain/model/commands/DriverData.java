package com.trazza.platform.iam.domain.model.commands;

import com.trazza.platform.iam.domain.model.valueobjects.DriverSex;

public record DriverData(String fullName, DriverSex sex, Integer experienceStars, boolean active) {
}
