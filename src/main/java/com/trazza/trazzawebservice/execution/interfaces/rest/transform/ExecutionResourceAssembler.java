package com.trazza.trazzawebservice.execution.interfaces.rest.transform;

import com.trazza.trazzawebservice.execution.domain.model.aggregates.DeviationAlert;
import com.trazza.trazzawebservice.execution.domain.model.aggregates.Incident;
import com.trazza.trazzawebservice.execution.domain.model.aggregates.TrackingPoint;
import com.trazza.trazzawebservice.execution.domain.model.entities.TripStatusChange;
import com.trazza.trazzawebservice.execution.domain.services.IotDeviceView;
import com.trazza.trazzawebservice.execution.domain.services.TrackingResult;
import com.trazza.trazzawebservice.execution.domain.services.TripView;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.DeviationAlertResource;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.IncidentEvidenceResource;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.IncidentResource;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.IotDeviceResource;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.TrackingPointResource;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.TrackingResultResource;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.TripPartyResource;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.TripPlaceResource;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.TripResource;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.TripStatusChangeResource;
import com.trazza.trazzawebservice.execution.interfaces.rest.resources.TripVehicleResource;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.Roles;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;

public final class ExecutionResourceAssembler {

    private ExecutionResourceAssembler() {
    }

    public static TripResource toResource(TripView view) {
        var trip = view.trip();
        var context = view.context();
        var showPhones = !trip.isClosed();
        return new TripResource(trip.getPublicId(), trip.getStatus().name(),
                view.isCarrier() ? Roles.CARRIER : Roles.MERCHANT, trip.getAgreedRate(), trip.getCurrency(),
                context.matchProposalPublicId(), context.returnRoutePublicId(), context.shipmentRequestPublicId(),
                context.description(), context.weightKg(), context.volumeM3(),
                place(context.originAddress(), context.origin()),
                place(context.destinationAddress(), context.destination()),
                place(context.pickupAddress(), context.pickup()), place(context.deliveryAddress(), context.delivery()),
                context.departureAt(),
                new TripPartyResource(view.carrierName(), showPhones ? view.carrierPhone() : null),
                new TripPartyResource(view.merchantName(), showPhones ? view.merchantPhone() : null),
                new TripVehicleResource(view.vehiclePublicId(), view.vehiclePlate()),
                trip.getDriverId() != null ? trip.getDriverId() : context.routeDriverId(), view.driverName(),
                view.lastPoint() == null ? null : toResource(view.lastPoint()), view.openAlerts(),
                trip.getTotalDistanceKm(), trip.getPickedUpAt(), trip.getDeliveredAt(), trip.getConfirmedAt(),
                trip.getCancelledAt(), trip.getCancellationReason(), trip.getCreatedAt());
    }

    public static TripStatusChangeResource toResource(TripStatusChange change) {
        return new TripStatusChangeResource(change.getFromStatus(), change.getToStatus(), change.getChangedAt());
    }

    public static TrackingPointResource toResource(TrackingPoint point) {
        return new TrackingPointResource(point.getId(), point.getLocation().latitude(), point.getLocation().longitude(),
                point.getSpeedKmh(), point.getSource().name(), point.getRecordedAt());
    }

    public static DeviationAlertResource toResource(DeviationAlert alert) {
        return new DeviationAlertResource(alert.getId(), alert.getLocation().latitude(), alert.getLocation().longitude(),
                alert.getDeviationKm(), alert.getDetectedAt(), alert.getAcknowledgedAt());
    }

    public static TrackingResultResource toResource(TrackingResult result) {
        return new TrackingResultResource(toResource(result.point()),
                result.alert() == null ? null : toResource(result.alert()), result.tripStatus());
    }

    public static IncidentResource toResource(Incident incident) {
        return new IncidentResource(incident.getId(), incident.getType().name(), incident.getDescription(),
                incident.getStatus().name(), incident.getResolution(), incident.getReportedAt(),
                incident.getResolvedAt(), incident.getEvidences().stream()
                .map(evidence -> new IncidentEvidenceResource(evidence.getId(), evidence.getS3Key(),
                        evidence.getUploadedAt()))
                .toList());
    }

    public static IotDeviceResource toResource(IotDeviceView view) {
        var device = view.device();
        return new IotDeviceResource(device.getId(), device.getSerialNumber(), device.getStatus().name(),
                view.vehiclePublicId(), view.vehiclePlate(), device.getLoanedAt());
    }

    private static TripPlaceResource place(String address, GeoLocation location) {
        return new TripPlaceResource(address, location.latitude(), location.longitude());
    }
}
