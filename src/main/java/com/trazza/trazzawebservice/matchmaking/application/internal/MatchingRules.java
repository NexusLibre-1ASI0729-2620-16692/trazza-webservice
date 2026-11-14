package com.trazza.trazzawebservice.matchmaking.application.internal;

import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ReturnRoute;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ShipmentRequest;
import com.trazza.trazzawebservice.matchmaking.domain.services.ReturnRouteView;

import java.time.Duration;

/** Rules that decide whether a shipment can travel on a return route. */
public final class MatchingRules {

    /** Max distance, in km, from the pick-up and the delivery to the straight route line. */
    public static final double CORRIDOR_KM = 25;

    /** A route may leave up to this many hours before the pick-up window opens. */
    public static final Duration EARLY_DEPARTURE = Duration.ofHours(24);

    private MatchingRules() {
    }

    public static boolean fitsSchedule(ReturnRoute route, ShipmentRequest shipment) {
        return !route.getDepartureAt().isAfter(shipment.getPickupUntil())
                && !route.getDepartureAt().isBefore(shipment.getPickupFrom().minus(EARLY_DEPARTURE));
    }

    public static boolean fitsCapacity(ReturnRouteView route, ShipmentRequest shipment) {
        return route.acceptedShipments() < route.route().getMaxShipments()
                && shipment.getWeightKg().compareTo(route.availableWeightKg()) <= 0
                && shipment.getVolumeM3().compareTo(route.availableVolumeM3()) <= 0;
    }

    /** Both points are near the route line and the pick-up comes before the delivery. */
    public static boolean fitsPath(ReturnRoute route, ShipmentRequest shipment) {
        var origin = route.getOrigin();
        var destination = route.getDestination();
        return shipment.getPickup().distanceToSegment(origin, destination) <= CORRIDOR_KM
                && shipment.getDelivery().distanceToSegment(origin, destination) <= CORRIDOR_KM
                && origin.distanceTo(shipment.getPickup()) <= origin.distanceTo(shipment.getDelivery());
    }

    public static boolean fits(ReturnRouteView route, ShipmentRequest shipment) {
        return fitsSchedule(route.route(), shipment) && fitsCapacity(route, shipment)
                && fitsPath(route.route(), shipment);
    }
}
