package com.trazza.platform.execution.interfaces.rest;

import com.trazza.platform.execution.domain.model.commands.AcknowledgeDeviationAlertCommand;
import com.trazza.platform.execution.domain.model.commands.AddIncidentEvidenceCommand;
import com.trazza.platform.execution.domain.model.commands.AssignDriverCommand;
import com.trazza.platform.execution.domain.model.commands.CancelTripCommand;
import com.trazza.platform.execution.domain.model.commands.ConfirmTripCommand;
import com.trazza.platform.execution.domain.model.commands.DeliverTripCommand;
import com.trazza.platform.execution.domain.model.commands.PickUpTripCommand;
import com.trazza.platform.execution.domain.model.commands.RecordTrackingPointCommand;
import com.trazza.platform.execution.domain.model.commands.ReportIncidentCommand;
import com.trazza.platform.execution.domain.model.commands.ResolveIncidentCommand;
import com.trazza.platform.execution.domain.model.valueobjects.IncidentType;
import com.trazza.platform.execution.domain.model.valueobjects.TrackingSource;
import com.trazza.platform.execution.domain.services.ExecutionQueryService;
import com.trazza.platform.execution.domain.services.IncidentCommandService;
import com.trazza.platform.execution.domain.services.TrackingCommandService;
import com.trazza.platform.execution.domain.services.TripCommandService;
import com.trazza.platform.execution.interfaces.rest.resources.AddIncidentEvidenceResource;
import com.trazza.platform.execution.interfaces.rest.resources.AssignDriverResource;
import com.trazza.platform.execution.interfaces.rest.resources.CancelTripResource;
import com.trazza.platform.execution.interfaces.rest.resources.RecordTrackingPointResource;
import com.trazza.platform.execution.interfaces.rest.resources.ReportIncidentResource;
import com.trazza.platform.execution.interfaces.rest.resources.ResolveIncidentResource;
import com.trazza.platform.execution.interfaces.rest.transform.ExecutionResourceAssembler;
import com.trazza.platform.iam.domain.model.valueobjects.Roles;
import com.trazza.platform.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.platform.iam.infrastructure.authorization.Authorize;
import com.trazza.platform.shared.domain.model.CommandError;
import com.trazza.platform.shared.domain.model.valueobjects.EnumText;
import com.trazza.platform.shared.interfaces.rest.RestResults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Trips of the signed-in user (as carrier or merchant): life cycle, GPS tracking, deviation alerts and incidents. */
@RestController
@RequestMapping(value = "/api/v1/trips", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize
@Tag(name = "Trips", description = "Trip life cycle, GPS tracking, alerts and incidents")
public class TripsController {

    private final TripCommandService tripCommandService;
    private final TrackingCommandService trackingCommandService;
    private final IncidentCommandService incidentCommandService;
    private final ExecutionQueryService queryService;
    private final RestResults results;

    public TripsController(TripCommandService tripCommandService, TrackingCommandService trackingCommandService,
                           IncidentCommandService incidentCommandService, ExecutionQueryService queryService,
                           RestResults results) {
        this.tripCommandService = tripCommandService;
        this.trackingCommandService = trackingCommandService;
        this.incidentCommandService = incidentCommandService;
        this.queryService = queryService;
        this.results = results;
    }

    @GetMapping
    @Operation(summary = "List my trips")
    public ResponseEntity<Object> listMine(AuthenticatedUser user) {
        return ResponseEntity.ok(queryService.listMyTrips(user.id()).stream()
                .map(ExecutionResourceAssembler::toResource).toList());
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get a trip")
    public ResponseEntity<Object> get(AuthenticatedUser user, @PathVariable UUID tripId) {
        return results.ok(queryService.getTrip(user.id(), tripId), ExecutionResourceAssembler::toResource);
    }

    @GetMapping("/{tripId}/status-history")
    @Operation(summary = "Status changes of a trip")
    public ResponseEntity<Object> history(AuthenticatedUser user, @PathVariable UUID tripId) {
        return results.ok(queryService.listHistory(user.id(), tripId),
                changes -> changes.stream().map(ExecutionResourceAssembler::toResource).toList());
    }

    @PutMapping("/{tripId}/driver")
    @Authorize(Roles.CARRIER)
    @Operation(summary = "Assign a driver")
    public ResponseEntity<Object> assignDriver(AuthenticatedUser user, @PathVariable UUID tripId,
                                               @RequestBody AssignDriverResource resource) {
        return results.ok(tripCommandService.handle(new AssignDriverCommand(user.id(), tripId, resource.driverId())),
                ExecutionResourceAssembler::toResource);
    }

    @PostMapping("/{tripId}/pick-up")
    @Authorize(Roles.CARRIER)
    @Operation(summary = "The cargo was picked up")
    public ResponseEntity<Object> pickUp(AuthenticatedUser user, @PathVariable UUID tripId) {
        return results.ok(tripCommandService.handle(new PickUpTripCommand(user.id(), tripId)),
                ExecutionResourceAssembler::toResource);
    }

    @PostMapping("/{tripId}/deliver")
    @Authorize(Roles.CARRIER)
    @Operation(summary = "The cargo was delivered")
    public ResponseEntity<Object> deliver(AuthenticatedUser user, @PathVariable UUID tripId) {
        return results.ok(tripCommandService.handle(new DeliverTripCommand(user.id(), tripId)),
                ExecutionResourceAssembler::toResource);
    }

    @PostMapping("/{tripId}/confirm")
    @Authorize(Roles.MERCHANT)
    @Operation(summary = "The merchant confirms the delivery")
    public ResponseEntity<Object> confirm(AuthenticatedUser user, @PathVariable UUID tripId) {
        return results.ok(tripCommandService.handle(new ConfirmTripCommand(user.id(), tripId)),
                ExecutionResourceAssembler::toResource);
    }

    @PostMapping("/{tripId}/cancel")
    @Operation(summary = "Cancel a trip that was not picked up")
    public ResponseEntity<Object> cancel(AuthenticatedUser user, @PathVariable UUID tripId,
                                         @RequestBody CancelTripResource resource) {
        return results.ok(tripCommandService.handle(new CancelTripCommand(user.id(), tripId, resource.reason())),
                ExecutionResourceAssembler::toResource);
    }

    @GetMapping("/{tripId}/tracking-points")
    @Operation(summary = "GPS points of a trip")
    public ResponseEntity<Object> listTrackingPoints(AuthenticatedUser user, @PathVariable UUID tripId) {
        return results.ok(queryService.listTrackingPoints(user.id(), tripId),
                points -> points.stream().map(ExecutionResourceAssembler::toResource).toList());
    }

    @PostMapping("/{tripId}/tracking-points")
    @Authorize(Roles.CARRIER)
    @Operation(summary = "GPS point from the phone, the GPS simulator or an IoT device (source PHONE or IOT)")
    public ResponseEntity<Object> recordTrackingPoint(AuthenticatedUser user, @PathVariable UUID tripId,
                                                      @RequestBody RecordTrackingPointResource resource) {
        var source = TrackingSource.PHONE;
        if (resource.source() != null && !resource.source().isBlank()) {
            var parsed = EnumText.tryParse(TrackingSource.class, resource.source());
            if (parsed.isEmpty()) return results.error(CommandError.validation("source-invalid"));
            source = parsed.get();
        }
        return results.created(trackingCommandService.handle(new RecordTrackingPointCommand(user.id(), tripId,
                        resource.latitude(), resource.longitude(), resource.speedKmh(), source, resource.recordedAt())),
                ExecutionResourceAssembler::toResource);
    }

    @GetMapping("/{tripId}/deviation-alerts")
    @Operation(summary = "Route deviation alerts of a trip")
    public ResponseEntity<Object> listDeviationAlerts(AuthenticatedUser user, @PathVariable UUID tripId) {
        return results.ok(queryService.listDeviationAlerts(user.id(), tripId),
                alerts -> alerts.stream().map(ExecutionResourceAssembler::toResource).toList());
    }

    @PostMapping("/{tripId}/deviation-alerts/{alertId}/acknowledge")
    @Operation(summary = "Mark a deviation alert as seen")
    public ResponseEntity<Object> acknowledgeDeviationAlert(AuthenticatedUser user, @PathVariable UUID tripId,
                                                            @PathVariable Long alertId) {
        return results.noContent(trackingCommandService.handle(
                new AcknowledgeDeviationAlertCommand(user.id(), tripId, alertId)));
    }

    @GetMapping("/{tripId}/incidents")
    @Operation(summary = "Incidents of a trip")
    public ResponseEntity<Object> listIncidents(AuthenticatedUser user, @PathVariable UUID tripId) {
        return results.ok(queryService.listIncidents(user.id(), tripId),
                incidents -> incidents.stream().map(ExecutionResourceAssembler::toResource).toList());
    }

    @PostMapping("/{tripId}/incidents")
    @Operation(summary = "Report an incident")
    public ResponseEntity<Object> reportIncident(AuthenticatedUser user, @PathVariable UUID tripId,
                                                 @RequestBody ReportIncidentResource resource) {
        var type = EnumText.tryParse(IncidentType.class, resource.type());
        if (type.isEmpty()) return results.error(CommandError.validation("incident-type-invalid"));
        return results.created(incidentCommandService.handle(
                        new ReportIncidentCommand(user.id(), tripId, type.get(), resource.description())),
                ExecutionResourceAssembler::toResource);
    }

    @PostMapping("/{tripId}/incidents/{incidentId}/evidences")
    @Operation(summary = "Attach a photo (S3 key) to an incident")
    public ResponseEntity<Object> addIncidentEvidence(AuthenticatedUser user, @PathVariable UUID tripId,
                                                      @PathVariable Long incidentId,
                                                      @RequestBody AddIncidentEvidenceResource resource) {
        return results.created(incidentCommandService.handle(
                        new AddIncidentEvidenceCommand(user.id(), tripId, incidentId, resource.s3Key())),
                ExecutionResourceAssembler::toResource);
    }

    @PostMapping("/{tripId}/incidents/{incidentId}/resolve")
    @Operation(summary = "Resolve an incident")
    public ResponseEntity<Object> resolveIncident(AuthenticatedUser user, @PathVariable UUID tripId,
                                                  @PathVariable Long incidentId,
                                                  @RequestBody ResolveIncidentResource resource) {
        return results.ok(incidentCommandService.handle(
                        new ResolveIncidentCommand(user.id(), tripId, incidentId, resource.resolution())),
                ExecutionResourceAssembler::toResource);
    }
}
