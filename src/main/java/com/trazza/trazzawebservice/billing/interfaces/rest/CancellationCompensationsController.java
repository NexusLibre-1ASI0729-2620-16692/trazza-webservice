package com.trazza.trazzawebservice.billing.interfaces.rest;

import com.trazza.trazzawebservice.billing.domain.model.commands.UpdateCompensationCommand;
import com.trazza.trazzawebservice.billing.domain.model.valueobjects.CompensationAction;
import com.trazza.trazzawebservice.billing.domain.services.BillingQueryService;
import com.trazza.trazzawebservice.billing.domain.services.SettlementCommandService;
import com.trazza.trazzawebservice.billing.interfaces.rest.transform.BillingResourceAssembler;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.Roles;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.shared.interfaces.rest.RestResults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Lost profit (30% of the agreed rate) when the merchant cancels after the route left. */
@RestController
@RequestMapping(value = "/api/v1/cancellation-compensations", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize
@Tag(name = "Cancellation compensations", description = "Lost profit after a late cancellation")
public class CancellationCompensationsController {

    private final SettlementCommandService commandService;
    private final BillingQueryService queryService;
    private final RestResults results;

    public CancellationCompensationsController(SettlementCommandService commandService,
                                               BillingQueryService queryService, RestResults results) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.results = results;
    }

    @GetMapping
    @Operation(summary = "List the compensations of my trips")
    public ResponseEntity<Object> listMine(AuthenticatedUser user) {
        return ResponseEntity.ok(queryService.listMyCompensations(user.id()).stream()
                .map(BillingResourceAssembler::toResource).toList());
    }

    @PostMapping("/{tripId}/confirm-paid")
    @Authorize(Roles.CARRIER)
    @Operation(summary = "The carrier received the compensation")
    public ResponseEntity<Object> confirmPaid(AuthenticatedUser user, @PathVariable UUID tripId) {
        return update(user, tripId, CompensationAction.CONFIRM_PAID);
    }

    @PostMapping("/{tripId}/waive")
    @Authorize(Roles.CARRIER)
    @Operation(summary = "The carrier forgives the compensation")
    public ResponseEntity<Object> waive(AuthenticatedUser user, @PathVariable UUID tripId) {
        return update(user, tripId, CompensationAction.WAIVE);
    }

    @PostMapping("/{tripId}/dispute")
    @Authorize(Roles.MERCHANT)
    @Operation(summary = "The merchant disputes the compensation")
    public ResponseEntity<Object> dispute(AuthenticatedUser user, @PathVariable UUID tripId) {
        return update(user, tripId, CompensationAction.DISPUTE);
    }

    private ResponseEntity<Object> update(AuthenticatedUser user, UUID tripId, CompensationAction action) {
        return results.ok(commandService.handle(new UpdateCompensationCommand(user.id(), tripId, action)),
                BillingResourceAssembler::toResource);
    }
}
