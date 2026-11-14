package com.trazza.trazzawebservice.reputation.domain.services;

import com.trazza.trazzawebservice.reputation.domain.model.aggregates.Rating;

import java.util.UUID;

public record RatingView(Rating rating, UUID tripPublicId, UUID counterpartPublicId, String counterpartName) {
}