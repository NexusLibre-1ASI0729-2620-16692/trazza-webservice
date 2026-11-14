package com.trazza.trazzawebservice.shared.domain.model.valueobjects;

import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;

import java.util.List;
import java.util.Locale;

/**
 * Geographic coordinates (WGS 84, SRID 4326) in decimal degrees.
 */
public record GeoLocation(double latitude, double longitude) {

    private static final double EARTH_RADIUS_KM = 6371;
    private static final double KM_PER_DEGREE = 111.32;

    public static Result<GeoLocation> create(Double latitude, Double longitude) {
        if (latitude == null || !Double.isFinite(latitude) || latitude < -90 || latitude > 90)
            return Result.failure(CommandError.validation("latitude-invalid"));
        if (longitude == null || !Double.isFinite(longitude) || longitude < -180 || longitude > 180)
            return Result.failure(CommandError.validation("longitude-invalid"));
        return Result.success(new GeoLocation(latitude, longitude));
    }

    /** Well-known text in latitude-longitude order, used with {@code ST_PointFromText(?, 4326, 'axis-order=lat-long')}. */
    public String toWkt() {
        return String.format(Locale.ROOT, "POINT(%s %s)", latitude, longitude);
    }

    /** Reads {@code POINT(lat lng)} as returned by {@code ST_AsText} for SRID 4326. */
    public static GeoLocation fromWkt(String wkt) {
        if (wkt == null || wkt.isBlank()) return null;
        var inner = wkt.substring(wkt.indexOf('(') + 1, wkt.lastIndexOf(')')).trim();
        var parts = inner.split("\\s+");
        return new GeoLocation(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
    }

    /** Great-circle distance in kilometers (haversine formula). */
    public double distanceTo(GeoLocation other) {
        var deltaLat = Math.toRadians(other.latitude - latitude);
        var deltaLng = Math.toRadians(other.longitude - longitude);
        var a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.pow(Math.sin(deltaLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    /**
     * Approximate distance in kilometers from this location to the segment between {@code start} and {@code end};
     * used to detect route deviations.
     */
    public double distanceToSegment(GeoLocation start, GeoLocation end) {
        var cosLat = Math.cos(latitude * Math.PI / 180);
        double px = longitude * KM_PER_DEGREE * cosLat, py = latitude * KM_PER_DEGREE;
        double ax = start.longitude * KM_PER_DEGREE * cosLat, ay = start.latitude * KM_PER_DEGREE;
        double bx = end.longitude * KM_PER_DEGREE * cosLat, by = end.latitude * KM_PER_DEGREE;
        var dx = bx - ax;
        var dy = by - ay;
        var lengthSquared = dx * dx + dy * dy;
        var t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / lengthSquared));
        return Math.sqrt(Math.pow(px - (ax + t * dx), 2) + Math.pow(py - (ay + t * dy), 2));
    }

    /** Smallest distance from this location to a path made of consecutive segments. */
    public double distanceToPath(List<GeoLocation> path) {
        var best = Double.MAX_VALUE;
        for (var i = 0; i < path.size() - 1; i++) best = Math.min(best, distanceToSegment(path.get(i), path.get(i + 1)));
        return best;
    }
}
