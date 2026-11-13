package com.trazza.iam.domain.model.valueobjects;

import com.trazza.shared.domain.model.CommandError;
import com.trazza.shared.domain.model.Result;

public class GeoLocation {
    private final double latitude;
    private final double longitude;

    private GeoLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public static Result<GeoLocation, CommandError> create(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return Result.failure(CommandError.validation("invalid-coordinates"));
        }
        return Result.success(new GeoLocation(lat, lon));
    }
}