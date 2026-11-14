package com.trazza.trazzawebservice.iam.domain.model.valueobjects;

import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalized (trimmed, lower case) e-mail address. */
public record EmailAddress(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    public static Result<EmailAddress> create(String value) {
        var normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.length() <= 255 && PATTERN.matcher(normalized).matches()
                ? Result.success(new EmailAddress(normalized))
                : Result.failure(CommandError.validation("email-invalid"));
    }
}
