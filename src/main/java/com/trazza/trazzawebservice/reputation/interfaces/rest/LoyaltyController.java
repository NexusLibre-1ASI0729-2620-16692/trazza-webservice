package com.trazza.trazzawebservice.reputation.interfaces.rest;

import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.reputation.domain.model.commands.RedeemPointsCommand;
import com.trazza.trazzawebservice.reputation.domain.services.ReputationCommandService;
import com.trazza.trazzawebservice.reputation.domain.services.ReputationQueryService;
import com.trazza.trazzawebservice.reputation.interfaces.rest.resources.RedeemPointsResource;
import com.trazza.trazzawebservice.reputation.interfaces.rest.transform.ReputationResourceAssembler;
import com.trazza.trazzawebservice.shared.interfaces.rest.RestResults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/loyalty", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize
@Tag(name = "Loyalty", description = "Loyalty points and redemptions")
public class LoyaltyController {

    private final ReputationCommandService commandService;
    private final ReputationQueryService queryService;
    private final RestResults results;

    public LoyaltyController(ReputationCommandService commandService, ReputationQueryService queryService,
                             RestResults results) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.results = results;
    }

    @GetMapping("/points")
    @Operation(summary = "My points balance and history")
    public ResponseEntity<Object> points(AuthenticatedUser user) {
        return ResponseEntity.ok(ReputationResourceAssembler.toResource(queryService.getLoyaltyAccount(user.id())));
    }

    @PostMapping("/redemptions")
    @Operation(summary = "Redeem points")
    public ResponseEntity<Object> redeem(AuthenticatedUser user, @RequestBody RedeemPointsResource resource) {
        return results.created(commandService.handle(new RedeemPointsCommand(user.id(), resource.points())),
                ReputationResourceAssembler::toResource);
    }
}