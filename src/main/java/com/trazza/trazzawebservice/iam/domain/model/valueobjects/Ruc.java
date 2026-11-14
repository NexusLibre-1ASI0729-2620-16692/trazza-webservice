package com.trazza.trazzawebservice.iam.domain.model.valueobjects;

import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;

import java.util.regex.Pattern;

/** Peruvian taxpayer number: eleven digits starting with 10 (person) or 20 (company). */
public record Ruc(String value) {

    private static final Pattern PATTERN = Pattern.compile("^(10|20)\\d{9}$");

    public static Result<Ruc> create(String value) {
        var normalized = value == null ? "" : value.trim();
        return PATTERN.matcher(normalized).matches()
                ? Result.success(new Ruc(normalized))
                : Result.failure(CommandError.validation("ruc-invalid"));
    }
}
