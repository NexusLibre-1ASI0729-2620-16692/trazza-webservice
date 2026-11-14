package com.trazza.platform.iam.domain.model.valueobjects;

import com.trazza.platform.shared.domain.model.CommandError;
import com.trazza.platform.shared.domain.model.Result;

import java.util.Locale;

/** Peruvian vehicle plate normalized as {@code ABC-123}. */
public record LicensePlate(String value) {

    public static Result<LicensePlate> create(String value) {
        var compact = (value == null ? "" : value.toUpperCase(Locale.ROOT)).replaceAll("[^A-Z0-9]", "");
        if (compact.length() != 6) return Result.failure(CommandError.validation("plate-invalid"));
        return Result.success(new LicensePlate(compact.substring(0, 3) + "-" + compact.substring(3)));
    }
}
