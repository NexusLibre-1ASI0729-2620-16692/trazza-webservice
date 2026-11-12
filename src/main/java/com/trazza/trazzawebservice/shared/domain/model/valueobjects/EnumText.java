package com.trazza.trazzawebservice.shared.domain.model.valueobjects;

import java.util.Optional;

/**
 * Lenient parsing of the UPPER_SNAKE_CASE text used by the database ENUM columns and the API.
 * Case and underscores are ignored, so {@code picked_up}, {@code PICKED_UP} and {@code PickedUp} are the same.
 */
public final class EnumText {

    private EnumText() {
    }

    public static <E extends Enum<E>> Optional<E> tryParse(Class<E> type, String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        var compact = text.trim().replace("_", "");
        for (var constant : type.getEnumConstants())
            if (constant.name().replace("_", "").equalsIgnoreCase(compact)) return Optional.of(constant);
        return Optional.empty();
    }
}
