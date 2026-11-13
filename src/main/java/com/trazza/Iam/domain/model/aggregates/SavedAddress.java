package com.trazza.iam.domain.model.aggregates;

import java.time.LocalDateTime;

public record SavedAddress(
        Long id,
        Long userId,
        String label,
        String address,
        String district,
        double latitude,
        double longitude,
        LocalDateTime createdAt) {
}