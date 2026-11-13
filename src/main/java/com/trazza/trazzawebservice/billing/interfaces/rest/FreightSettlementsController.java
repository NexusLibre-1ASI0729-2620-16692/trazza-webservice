package com.trazza.trazzawebservice.billing.interfaces.rest;

import com.trazza.trazzawebservice.billing.domain.model.commands.ConfirmFreightReceivedCommand;
import com.trazza.trazzawebservice.billing.domain.model.commands.MarkFreightPaidCommand;
import com.trazza.trazzawebservice.billing.domain.services.BillingQueryService;
import com.trazza.trazzawebservice.billing.domain.services.SettlementCommandService;
import com.trazza.trazzawebservice.billing.interfaces.rest.resources.MarkFreightPaidResource;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/freight-settlements", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize
@Tag(name = "Freight settlements", description = "Freight paid outside Trazza")
public class FreightSettlementsController {

    private final SettlementCommandService commandService;
    private final BillingQueryService queryService;
    private final RestResults results;

    public FreightSettlementsController(SettlementCommandService commandService, BillingQueryService queryService,
                                        RestResults results) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.results = results;
    }

    @GetMapping
    @Operation(summary = "List the freight settlements of my trips")
    public ResponseEntity<Object> listMine(AuthenticatedUser user) {
        return ResponseEntity.ok(queryService.listMySettlements(user.id()).stream()
                .map(BillingResourceAssembler::toResource).toList());
    }

    @PostMapping("/{tripId}/mark-paid")
    @Authorize(Roles.MERCHANT)
    @Operation(summary = "The merchant says the freight was paid")
    public ResponseEntity<Object> markPaid(AuthenticatedUser user, @PathVariable UUID tripId,
                                           @RequestBody(required = false) MarkFreightPaidResource resource) {
        return results.ok(commandService.handle(new MarkFreightPaidCommand(user.id(), tripId,
                resource == null ? null : resource.externalReference())), BillingResourceAssembler::toResource);
    }

    @PostMapping("/{tripId}/confirm")
    @Authorize(Roles.CARRIER)
    @Operation(summary = "The carrier confirms it received the freight")
    public ResponseEntity<Object> confirm(AuthenticatedUser user, @PathVariable UUID tripId) {
        return results.ok(commandService.handle(new ConfirmFreightReceivedCommand(user.id(), tripId)),
                BillingResourceAssembler::toResource);
    }
}