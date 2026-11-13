package com.trazza.trazzawebservice.matchmaking.interfaces.rest;

import com.trazza.trazzawebservice.iam.domain.model.valueobjects.Roles;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.ReturnRoute;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CancelReturnRouteCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CreateReturnRouteCommand;
import com.trazza.trazzawebservice.matchmaking.domain.services.MatchmakingQueryService;
import com.trazza.trazzawebservice.matchmaking.domain.services.ReturnRouteCommandService;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.CreateReturnRouteResource;
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

/** Return routes published by the signed-in carrier (max 2 shipments each) and the shipments that fit them. */
@RestController
@RequestMapping(value = "/api/v1/return-routes", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize(Roles.CARRIER)
@Tag(name = "Return routes", description = "Empty return trips of the signed-in carrier")
public class ReturnRoutesController {

    private final ReturnRouteCommandService commandService;
    private final MatchmakingQueryService queryService;
    private final RestResults results;

    public ReturnRoutesController(ReturnRouteCommandService commandService, MatchmakingQueryService queryService,
                                  RestResults results) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.results = results;
    }

    @GetMapping
    @Operation(summary = "List my return routes")
    public ResponseEntity<Object> listMine(AuthenticatedUser user) {
        return ResponseEntity.ok(queryService.listMyRoutes(user.id()).stream()
                .map(MatchmakingResourceAssembler::toResource).toList());
    }

    @GetMapping("/{routeId}")
    @Operation(summary = "Get one of my return routes")
    public ResponseEntity<Object> get(AuthenticatedUser user, @PathVariable UUID routeId) {
        return results.ok(queryService.getRoute(user.id(), routeId), MatchmakingResourceAssembler::toResource);
    }

    @PostMapping
    @Operation(summary = "Publish a return route")
    public ResponseEntity<Object> create(AuthenticatedUser user, @RequestBody CreateReturnRouteResource resource) {
        return results.created(commandService.handle(new CreateReturnRouteCommand(user.id(), resource.vehicleId(),
                        resource.driverId(), MatchmakingResourceAssembler.toPlace(resource.origin()),
                        MatchmakingResourceAssembler.toPlace(resource.destination()), resource.departureAt(),
                        resource.maxShipments() == null ? ReturnRoute.MAX_SHIPMENTS_LIMIT : resource.maxShipments())),
                MatchmakingResourceAssembler::toResource);
    }

    @PostMapping("/{routeId}/cancel")
    @Operation(summary = "Cancel a return route without accepted shipments")
    public ResponseEntity<Object> cancel(AuthenticatedUser user, @PathVariable UUID routeId) {
        return results.ok(commandService.handle(new CancelReturnRouteCommand(user.id(), routeId)),
                MatchmakingResourceAssembler::toResource);
    }

    @GetMapping("/{routeId}/suggested-shipments")
    @Operation(summary = "Open shipments that fit the route")
    public ResponseEntity<Object> suggestShipments(AuthenticatedUser user, @PathVariable UUID routeId) {
        return results.ok(queryService.suggestShipments(user.id(), routeId),
                shipments -> shipments.stream().map(MatchmakingResourceAssembler::toResource).toList());
    }
}
