package com.trazza.trazzawebservice.billing.interfaces.rest;

import com.trazza.trazzawebservice.billing.domain.model.commands.CancelSubscriptionCommand;
import com.trazza.trazzawebservice.billing.domain.model.commands.RenewSubscriptionCommand;
import com.trazza.trazzawebservice.billing.domain.model.commands.SubscribeCommand;
import com.trazza.trazzawebservice.billing.domain.services.BillingQueryService;
import com.trazza.trazzawebservice.billing.domain.services.SubscriptionCommandService;
import com.trazza.trazzawebservice.billing.interfaces.rest.resources.SubscribeResource;
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

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize(Roles.CARRIER)
@Tag(name = "Subscriptions", description = "Monitoring plans of my trucks")
public class SubscriptionsController {

    private final SubscriptionCommandService commandService;
    private final BillingQueryService queryService;
    private final RestResults results;

    public SubscriptionsController(SubscriptionCommandService commandService, BillingQueryService queryService,
                                   RestResults results) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.results = results;
    }

    @GetMapping
    @Operation(summary = "List my subscriptions")
    public ResponseEntity<Object> listMine(AuthenticatedUser user) {
        return ResponseEntity.ok(queryService.listMySubscriptions(user.id()).stream()
                .map(BillingResourceAssembler::toResource).toList());
    }

    @PostMapping
    @Operation(summary = "Subscribe trucks to a plan (simulated payment)")
    public ResponseEntity<Object> subscribe(AuthenticatedUser user, @RequestBody SubscribeResource resource) {
        return results.created(commandService.handle(new SubscribeCommand(user.id(), resource.planCode(),
                        resource.vehicleIds() == null ? List.of() : resource.vehicleIds())),
                BillingResourceAssembler::toResource);
    }

    @PostMapping("/{subscriptionId}/renew")
    @Operation(summary = "Pay the next month")
    public ResponseEntity<Object> renew(AuthenticatedUser user, @PathVariable Long subscriptionId) {
        return results.ok(commandService.handle(new RenewSubscriptionCommand(user.id(), subscriptionId)),
                BillingResourceAssembler::toResource);
    }

    @PostMapping("/{subscriptionId}/cancel")
    @Operation(summary = "Cancel a subscription (IoT devices are returned)")
    public ResponseEntity<Object> cancel(AuthenticatedUser user, @PathVariable Long subscriptionId) {
        return results.ok(commandService.handle(new CancelSubscriptionCommand(user.id(), subscriptionId)),
                BillingResourceAssembler::toResource);
    }
}
