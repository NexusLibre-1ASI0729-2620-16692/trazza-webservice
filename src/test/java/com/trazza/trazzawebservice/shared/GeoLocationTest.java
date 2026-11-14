package com.trazza.trazzawebservice.shared;

import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoLocationTest {

    private final GeoLocation callao = new GeoLocation(-12.048, -77.1);
    private final GeoLocation surco = new GeoLocation(-12.086, -76.978);

    @Test
    void wellKnownTextKeepsLatitudeFirst() {
        assertEquals("POINT(-12.048 -77.1)", callao.toWkt());
        assertEquals(callao, GeoLocation.fromWkt(callao.toWkt()));
    }

    @Test
    void distanceBetweenCallaoAndSurcoIsAbout14Km() {
        var km = callao.distanceTo(surco);
        assertTrue(km > 13 && km < 15, "distance was " + km);
    }

    @Test
    void rejectsLatitudeOutOfRange() {
        assertTrue(GeoLocation.create(95.0, -77.0).isFailure());
        assertTrue(GeoLocation.create(-12.0, -77.0).isSuccess());
    }
}
