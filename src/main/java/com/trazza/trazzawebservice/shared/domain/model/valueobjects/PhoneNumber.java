package com.trazza.trazzawebservice.shared.domain.model.valueobjects;

import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;

import java.util.regex.Pattern;

/**
 * Peruvian mobile number: 9 digits starting with 9 (an optional +51 prefix is removed).
 */
public record PhoneNumber(String value) {

    private static final Pattern MOBILE = Pattern.compile("^9\\d{8}$");

    public static Result<PhoneNumber> create(String value) {
        var digits = (value == null ? "" : value).replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("519")) digits = digits.substring(2);
        return MOBILE.matcher(digits).matches()
                ? Result.success(new PhoneNumber(digits))
                : Result.failure(CommandError.validation("phone-invalid"));
    }

    @Override
    public String toString() {
        return value;
    }
}
