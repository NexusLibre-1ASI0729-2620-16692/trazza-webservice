package com.trazza.iam.domain.model.aggregates;

import java.time.LocalDateTime;

/**
 * Frequent address of a user (saved_addresses), e.g. "Almacén" or "Tienda Gamarra".
 */
public record SavedAddress(
        Long id,
        Long userId,
        String label,
        String address,
        String district,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt
) {}