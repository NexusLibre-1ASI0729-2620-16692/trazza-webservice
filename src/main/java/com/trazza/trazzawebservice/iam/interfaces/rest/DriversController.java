package com.trazza.trazzawebservice.iam.interfaces.rest;

import com.trazza.trazzawebservice.iam.domain.model.commands.CreateDriverCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.DeactivateDriverCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateDriverCommand;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.Roles;
import com.trazza.trazzawebservice.iam.domain.services.FleetCommandService;
import com.trazza.trazzawebservice.iam.domain.services.FleetQueryService;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.iam.interfaces.rest.resources.SaveDriverResource;
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

/** Drivers employed by the signed-in carrier. */
@RestController
@RequestMapping(value = "/api/v1/drivers", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize(Roles.CARRIER)
@Tag(name = "Drivers", description = "Drivers of the signed-in carrier")
public class DriversController {

    private final FleetCommandService fleetCommandService;
    private final FleetQueryService fleetQueryService;
    private final RestResults results;

    public DriversController(FleetCommandService fleetCommandService, FleetQueryService fleetQueryService,
                             RestResults results) {
        this.fleetCommandService = fleetCommandService;
        this.fleetQueryService = fleetQueryService;
        this.results = results;
    }

    @GetMapping
    @Operation(summary = "List my drivers")
    public ResponseEntity<Object> list(AuthenticatedUser user) {
        return ResponseEntity.ok(fleetQueryService.listDrivers(user.id()).stream()
                .map(IamResourceAssembler::toResource).toList());
    }

    @PostMapping
    @Operation(summary = "Register a driver")
    public ResponseEntity<Object> create(AuthenticatedUser user, @RequestBody SaveDriverResource resource) {
        var data = IamResourceAssembler.toDriverData(resource);
        if (data.isFailure()) return results.error(data.error());
        return results.created(fleetCommandService.handle(new CreateDriverCommand(user.id(), data.value())),
                IamResourceAssembler::toResource);
    }

    @PutMapping("/{driverId}")
    @Operation(summary = "Update a driver")
    public ResponseEntity<Object> update(AuthenticatedUser user, @PathVariable Long driverId,
                                         @RequestBody SaveDriverResource resource) {
        var data = IamResourceAssembler.toDriverData(resource);
        if (data.isFailure()) return results.error(data.error());
        return results.ok(fleetCommandService.handle(new UpdateDriverCommand(user.id(), driverId, data.value())),
                IamResourceAssembler::toResource);
    }

    @DeleteMapping("/{driverId}")
    @Operation(summary = "Deactivate a driver")
    public ResponseEntity<Object> deactivate(AuthenticatedUser user, @PathVariable Long driverId) {
        return results.noContent(fleetCommandService.handle(new DeactivateDriverCommand(user.id(), driverId)));
    }
}
