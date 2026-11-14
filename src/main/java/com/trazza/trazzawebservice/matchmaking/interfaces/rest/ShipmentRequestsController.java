package com.trazza.trazzawebservice.matchmaking.interfaces.rest;

import com.trazza.trazzawebservice.iam.domain.model.valueobjects.Roles;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CancelShipmentRequestCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CreateShipmentRequestCommand;
import com.trazza.trazzawebservice.matchmaking.domain.services.MatchmakingQueryService;
import com.trazza.trazzawebservice.matchmaking.domain.services.ShipmentRequestCommandService;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.CancelWithReasonResource;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.CreateShipmentRequestResource;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.transform.MatchmakingResourceAssembler;
import com.trazza.trazzawebservice.shared.interfaces.rest.RestResults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Shipments published by merchants. Carriers browse the open ones. */
@RestController
@RequestMapping(value = "/api/v1/shipment-requests", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize
@Tag(name = "Shipment requests", description = "Cargo merchants need to move")
public class ShipmentRequestsController {

    private final ShipmentRequestCommandService commandService;
    private final MatchmakingQueryService queryService;
    private final RestResults results;

    public ShipmentRequestsController(ShipmentRequestCommandService commandService,
                                      MatchmakingQueryService queryService, RestResults results) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.results = results;
    }

    @GetMapping
    @Authorize(Roles.MERCHANT)
    @Operation(summary = "List my shipment requests")
    public ResponseEntity<Object> listMine(AuthenticatedUser user) {
        return ResponseEntity.ok(queryService.listMyShipments(user.id()).stream()
                .map(MatchmakingResourceAssembler::toResource).toList());
    }

    @GetMapping("/open")
    @Authorize(Roles.CARRIER)
    @Operation(summary = "Browse open shipment requests")
    public ResponseEntity<Object> listOpen() {
        return ResponseEntity.ok(queryService.listOpenShipments().stream()
                .map(MatchmakingResourceAssembler::toResource).toList());
    }

    @PostMapping
    @Authorize(Roles.MERCHANT)
    @Operation(summary = "Publish a shipment request")
    public ResponseEntity<Object> create(AuthenticatedUser user, @RequestBody CreateShipmentRequestResource resource) {
        return results.created(commandService.handle(new CreateShipmentRequestCommand(user.id(), resource.description(),
                        resource.weightKg(), resource.volumeM3(), MatchmakingResourceAssembler.toPlace(resource.pickup()),
                        MatchmakingResourceAssembler.toPlace(resource.delivery()), resource.pickupFrom(),
                        resource.pickupUntil())),
                MatchmakingResourceAssembler::toResource);
    }

    @PostMapping("/{shipmentId}/cancel")
    @Authorize(Roles.MERCHANT)
    @Operation(summary = "Cancel an open shipment request")
    public ResponseEntity<Object> cancel(AuthenticatedUser user, @PathVariable UUID shipmentId,
                                         @RequestBody CancelWithReasonResource resource) {
        return results.ok(commandService.handle(new CancelShipmentRequestCommand(user.id(), shipmentId,
                resource.reason())), MatchmakingResourceAssembler::toResource);
    }

    @GetMapping("/{shipmentId}/suggested-routes")
    @Authorize(Roles.MERCHANT)
    @Operation(summary = "Published routes that could carry the shipment")
    public ResponseEntity<Object> suggestRoutes(AuthenticatedUser user, @PathVariable UUID shipmentId) {
        return results.ok(queryService.suggestRoutes(user.id(), shipmentId),
                routes -> routes.stream().map(MatchmakingResourceAssembler::toResource).toList());
    }
}
