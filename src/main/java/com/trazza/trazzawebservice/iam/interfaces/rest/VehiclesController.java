package com.trazza.trazzawebservice.iam.interfaces.rest;

import com.trazza.trazzawebservice.iam.domain.model.commands.CreateVehicleCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.DeactivateVehicleCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateVehicleCommand;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.Roles;
import com.trazza.trazzawebservice.iam.domain.services.FleetCommandService;
import com.trazza.trazzawebservice.iam.domain.services.FleetQueryService;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.iam.interfaces.rest.resources.SaveVehicleResource;
import com.trazza.trazzawebservice.iam.interfaces.rest.transform.IamResourceAssembler;
import com.trazza.trazzawebservice.shared.interfaces.rest.RestResults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Trucks of the signed-in carrier. Identified by their public id. */
@RestController
@RequestMapping(value = "/api/v1/vehicles", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize(Roles.CARRIER)
@Tag(name = "Vehicles", description = "Trucks of the signed-in carrier")
public class VehiclesController {

    private final FleetCommandService fleetCommandService;
    private final FleetQueryService fleetQueryService;
    private final RestResults results;

    public VehiclesController(FleetCommandService fleetCommandService, FleetQueryService fleetQueryService,
                              RestResults results) {
        this.fleetCommandService = fleetCommandService;
        this.fleetQueryService = fleetQueryService;
        this.results = results;
    }

    @GetMapping
    @Operation(summary = "List my trucks")
    public ResponseEntity<Object> list(AuthenticatedUser user) {
        return ResponseEntity.ok(fleetQueryService.listVehicles(user.id()).stream()
                .map(IamResourceAssembler::toResource).toList());
    }

    @PostMapping
    @Operation(summary = "Register a truck")
    public ResponseEntity<Object> create(AuthenticatedUser user, @RequestBody SaveVehicleResource resource) {
        return results.created(fleetCommandService.handle(
                new CreateVehicleCommand(user.id(), IamResourceAssembler.toVehicleData(resource))),
                IamResourceAssembler::toResource);
    }

    @PutMapping("/{vehicleId}")
    @Operation(summary = "Update a truck")
    public ResponseEntity<Object> update(AuthenticatedUser user, @PathVariable UUID vehicleId,
                                         @RequestBody SaveVehicleResource resource) {
        return results.ok(fleetCommandService.handle(
                new UpdateVehicleCommand(user.id(), vehicleId, IamResourceAssembler.toVehicleData(resource))),
                IamResourceAssembler::toResource);
    }

    @DeleteMapping("/{vehicleId}")
    @Operation(summary = "Deactivate a truck")
    public ResponseEntity<Object> deactivate(AuthenticatedUser user, @PathVariable UUID vehicleId) {
        return results.noContent(fleetCommandService.handle(new DeactivateVehicleCommand(user.id(), vehicleId)));
    }
}
