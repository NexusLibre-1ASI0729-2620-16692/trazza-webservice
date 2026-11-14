package com.trazza.trazzawebservice.matchmaking.domain.services;

import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ReturnRoute;

import java.math.BigDecimal;
import java.util.UUID;

/** Return route with what the screens need: truck, driver and free capacity. */
public record ReturnRouteView(ReturnRoute route, UUID vehiclePublicId, String vehiclePlate, String vehicleModel,
                              String driverName, int acceptedShipments, BigDecimal availableWeightKg,
                              BigDecimal availableVolumeM3, int pendingProposals) {
}
