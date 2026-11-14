package com.trazza.trazzawebservice.matchmaking.interfaces.acl;

import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** What Execution needs to know about the route and the shipment behind a trip. */
public record TripContextSnapshot(Long matchProposalId, UUID matchProposalPublicId, Long returnRouteId,
                                  UUID returnRoutePublicId, Long carrierProfileId, Long vehicleId, Long routeDriverId,
                                  String originAddress, GeoLocation origin, String destinationAddress,
                                  GeoLocation destination, Instant departureAt, Long shipmentRequestId,
                                  UUID shipmentRequestPublicId, Long merchantProfileId, String description,
                                  BigDecimal weightKg, BigDecimal volumeM3, String pickupAddress, GeoLocation pickup,
                                  String deliveryAddress, GeoLocation delivery) {
}
