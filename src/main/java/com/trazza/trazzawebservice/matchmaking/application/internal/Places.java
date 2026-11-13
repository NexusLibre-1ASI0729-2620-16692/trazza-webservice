package com.trazza.trazzawebservice.matchmaking.application.internal;

import com.trazza.trazzawebservice.matchmaking.domain.model.commands.PlaceData;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;

/** Validation of an address with its coordinates. */
public final class Places {

    private Places() {
    }

    public static Result<GeoLocation> validate(PlaceData place) {
        if (place == null || place.address() == null || place.address().isBlank() || place.address().trim().length() > 255)
            return Result.failure(CommandError.validation("address-invalid"));
        return GeoLocation.create(place.latitude(), place.longitude());
    }
}
