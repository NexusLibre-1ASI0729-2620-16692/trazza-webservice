package com.trazza.trazzawebservice.iam.domain.model.valueobjects;

import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;

import java.util.regex.Pattern;

/** Peruvian national identity document: exactly eight digits. */
public record Dni(String value) {

    private static final Pattern PATTERN = Pattern.compile("^\\d{8}$");

    public static Result<Dni> create(String value) {
        var normalized = value == null ? "" : value.trim();
        return PATTERN.matcher(normalized).matches()
                ? Result.success(new Dni(normalized))
                : Result.failure(CommandError.validation("dni-invalid"));
    }

    public String lastThree() {
        return value.substring(value.length() - 3);
    }
}
