package com.trazza.iam.domain.model.valueobjects;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class IdentityDocuments {

    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern RUC_PATTERN = Pattern.compile("^(10|20)\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]");

    /**
     * Peruvian national identity document: exactly eight digits.
     */
    public record Dni(String value) {
        // Método de fábrica estático
        public static Optional<Dni> create(String value) {
            String normalized = value == null ? "" : value.trim();
            return DNI_PATTERN.matcher(normalized).matches()
                    ? Optional.of(new Dni(normalized))
                    : Optional.empty();
        }

        // Propiedad calculada (equivalente a LastThree)
        public String lastThree() {
            if (value == null || value.length() < 3) return "";
            return value.substring(value.length() - 3);
        }
    }

    /**
     * Peruvian taxpayer number: eleven digits starting with 10 (person) or 20 (company).
     */
    public record Ruc(String value) {
        public static Optional<Ruc> create(String value) {
            String normalized = value == null ? "" : value.trim();
            return RUC_PATTERN.matcher(normalized).matches()
                    ? Optional.of(new Ruc(normalized))
                    : Optional.empty();
        }
    }

    /**
     * Normalized e-mail address.
     */
    public record EmailAddress(String value) {
        public static Optional<EmailAddress> create(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            return (normalized.length() <= 255 && EMAIL_PATTERN.matcher(normalized).matches())
                    ? Optional.of(new EmailAddress(normalized))
                    : Optional.empty();
        }
    }

    /**
     * Peruvian vehicle plate normalized as ABC-123.
     */
    public record LicensePlate(String value) {
        public static Optional<LicensePlate> create(String value) {
            String upper = value == null ? "" : value.toUpperCase(Locale.ROOT);
            String compact = NON_ALPHANUMERIC.matcher(upper).replaceAll("");

            if (compact.length() != 6) return Optional.empty();

            String formatted = compact.substring(0, 3) + "-" + compact.substring(3);
            return Optional.of(new LicensePlate(formatted));
        }
    }
}