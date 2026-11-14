package com.trazza.trazzawebservice.reputation.interfaces.rest;

import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.reputation.domain.model.commands.RateTripCommand;
import com.trazza.trazzawebservice.reputation.domain.services.ReputationCommandService;
import com.trazza.trazzawebservice.reputation.domain.services.ReputationQueryService;
import com.trazza.trazzawebservice.reputation.interfaces.rest.resources.RateTripResource;
import com.trazza.trazzawebservice.reputation.interfaces.rest.transform.ReputationResourceAssembler;
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

@RestController
@RequestMapping(value = "/api/v1/ratings", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize
@Tag(name = "Ratings", description = "Ratings after a confirmed trip")
public class RatingsController {

    private final ReputationCommandService commandService;
    private final ReputationQueryService queryService;
    private final RestResults results;

    public RatingsController(ReputationCommandService commandService, ReputationQueryService queryService,
                             RestResults results) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.results = results;
    }

    @PostMapping
    @Operation(summary = "Rate the other party of a confirmed trip")
    public ResponseEntity<Object> rate(AuthenticatedUser user, @RequestBody RateTripResource resource) {
        return results.created(commandService.handle(new RateTripCommand(user.id(), resource.tripId(),
                resource.score(), resource.comment())), ReputationResourceAssembler::toResource);
    }

    @GetMapping("/received")
    @Operation(summary = "Ratings I received")
    public ResponseEntity<Object> received(AuthenticatedUser user) {
        return ResponseEntity.ok(queryService.listReceived(user.id()).stream()
                .map(ReputationResourceAssembler::toResource).toList());
    }

    @GetMapping("/given")
    @Operation(summary = "Ratings I gave")
    public ResponseEntity<Object> given(AuthenticatedUser user) {
        return ResponseEntity.ok(queryService.listGiven(user.id()).stream()
                .map(ReputationResourceAssembler::toResource).toList());
    }

    @GetMapping("/summary/{userId}")
    @Operation(summary = "Average score of any user, by public id")
    public ResponseEntity<Object> summary(@PathVariable UUID userId) {
        return results.ok(queryService.getSummary(userId), ReputationResourceAssembler::toResource);
    }
}