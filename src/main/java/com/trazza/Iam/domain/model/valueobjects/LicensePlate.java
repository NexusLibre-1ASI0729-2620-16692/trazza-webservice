package com.trazza.iam.domain.model.valueobjects;

import com.trazza.shared.domain.model.CommandError;
import com.trazza.shared.domain.model.Result;

public class LicensePlate {
    private final String value;

    private LicensePlate(String value) { this.value = value; }
    public String getValue() { return value; }

    public static Result<LicensePlate, CommandError> create(String raw) {
        if (raw == null || raw.trim().length() < 6 || raw.trim().length() > 10) {
            return Result.failure(CommandError.validation("invalid-plate"));
        }
        return Result.success(new LicensePlate(raw.trim().toUpperCase()));
    }
}