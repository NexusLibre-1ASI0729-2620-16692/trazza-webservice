package com.trazza.trazzawebservice.iam.interfaces.rest;

import com.trazza.trazzawebservice.iam.domain.model.commands.CreateSavedAddressCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.DeleteSavedAddressCommand;
import com.trazza.trazzawebservice.iam.domain.services.FleetCommandService;
import com.trazza.trazzawebservice.iam.domain.services.FleetQueryService;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.iam.interfaces.rest.resources.CreateSavedAddressResource;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frequent pick-up and delivery addresses of the signed-in user, with real coordinates. */
@RestController
@RequestMapping(value = "/api/v1/saved-addresses", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize
@Tag(name = "Saved addresses", description = "Frequent addresses of the signed-in user")
public class SavedAddressesController {

    private final FleetCommandService fleetCommandService;
    private final FleetQueryService fleetQueryService;
    private final RestResults results;

    public SavedAddressesController(FleetCommandService fleetCommandService, FleetQueryService fleetQueryService,
                                    RestResults results) {
        this.fleetCommandService = fleetCommandService;
        this.fleetQueryService = fleetQueryService;
        this.results = results;
    }

    @GetMapping
    @Operation(summary = "List my saved addresses")
    public ResponseEntity<Object> list(AuthenticatedUser user) {
        return ResponseEntity.ok(fleetQueryService.listSavedAddresses(user.id()).stream()
                .map(IamResourceAssembler::toResource).toList());
    }

    @PostMapping
    @Operation(summary = "Save an address")
    public ResponseEntity<Object> create(AuthenticatedUser user, @RequestBody CreateSavedAddressResource resource) {
        return results.created(fleetCommandService.handle(new CreateSavedAddressCommand(user.id(), resource.label(),
                resource.address(), resource.district(), resource.latitude(), resource.longitude())),
                IamResourceAssembler::toResource);
    }

    @DeleteMapping("/{savedAddressId}")
    @Operation(summary = "Delete a saved address")
    public ResponseEntity<Object> delete(AuthenticatedUser user, @PathVariable Long savedAddressId) {
        return results.noContent(fleetCommandService.handle(new DeleteSavedAddressCommand(user.id(), savedAddressId)));
    }
}
