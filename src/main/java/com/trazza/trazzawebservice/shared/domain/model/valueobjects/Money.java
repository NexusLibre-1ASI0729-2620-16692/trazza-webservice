package com.trazza.trazzawebservice.shared.domain.model.valueobjects;

import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Non-negative amount of money rounded to cents. Freight rates use soles (PEN) and Trazza plans use dollars (USD).
 */
public record Money(BigDecimal amount, String currency) {

    public static final String SOLES = "PEN";
    public static final String DOLLARS = "USD";
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(SOLES, DOLLARS);

    public static Result<Money> create(BigDecimal amount, String currency) {
        if (amount == null || amount.signum() < 0) return Result.failure(CommandError.validation("money-invalid"));
        if (currency == null || !SUPPORTED_CURRENCIES.contains(currency))
            return Result.failure(CommandError.validation("currency-invalid"));
        return Result.success(new Money(amount.setScale(2, RoundingMode.HALF_UP), currency));
    }
}
