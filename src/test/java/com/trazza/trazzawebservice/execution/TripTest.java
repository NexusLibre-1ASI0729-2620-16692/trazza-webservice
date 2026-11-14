package com.trazza.trazzawebservice.execution;

import com.trazza.trazzawebservice.execution.domain.model.aggregates.Trip;
import com.trazza.trazzawebservice.execution.domain.model.valueobjects.TripStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripTest {

    private Trip newTrip() {
        return new Trip(1L, null, new BigDecimal("120.00"), "PEN", 10L, Instant.now());
    }

    @Test
    void followsTheWholeLifecycle() {
        var trip = newTrip();
        assertTrue(trip.pickUp(10L, Instant.now()).isSuccess());
        trip.registerMovement(1.234, 10L, Instant.now());
        assertEquals(TripStatus.IN_TRANSIT, trip.getStatus());
        assertEquals(new BigDecimal("1.23"), trip.getTotalDistanceKm());
        assertTrue(trip.deliver(10L, Instant.now()).isSuccess());
        assertTrue(trip.confirm(20L, Instant.now()).isSuccess());
        assertTrue(trip.isClosed());
        assertEquals(5, trip.getHistory().size());
    }

    @Test
    void cannotBeCancelledAfterThePickUp() {
        var trip = newTrip();
        trip.pickUp(10L, Instant.now());
        var result = trip.cancel(20L, "Ya no", Instant.now());
        assertTrue(result.isFailure());
        assertEquals("trip-status-invalid", result.error().code());
    }

    @Test
    void cancellationNeedsAReason() {
        var result = newTrip().cancel(20L, "  ", Instant.now());
        assertEquals("cancellation-reason-required", result.error().code());
    }
}
