package com.trazza.trazzawebservice.matchmaking.interfaces.rest;

import com.trazza.trazzawebservice.iam.domain.model.valueobjects.Roles;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.AcceptMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CancelMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CreateMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.RejectMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.SendMessageCommand;
import com.trazza.trazzawebservice.matchmaking.domain.services.MatchProposalCommandService;
import com.trazza.trazzawebservice.matchmaking.domain.services.MatchmakingQueryService;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.CreateMatchProposalResource;
import com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources.SendMessageResource;
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

/** Single offers (no counter-offers) and the temporary chat between carrier and merchant. */
@RestController
@RequestMapping(value = "/api/v1/match-proposals", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize
@Tag(name = "Match proposals", description = "Offers and chat between carrier and merchant")
public class MatchProposalsController {

    private final MatchProposalCommandService commandService;
    private final MatchmakingQueryService queryService;
    private final RestResults results;

    public MatchProposalsController(MatchProposalCommandService commandService, MatchmakingQueryService queryService,
                                    RestResults results) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.results = results;
    }

    @GetMapping
    @Operation(summary = "List my proposals (as carrier or merchant)")
    public ResponseEntity<Object> listMine(AuthenticatedUser user) {
        return ResponseEntity.ok(queryService.listMyProposals(user.id()).stream()
                .map(MatchmakingResourceAssembler::toResource).toList());
    }

    @GetMapping("/{proposalId}")
    @Operation(summary = "Get a proposal")
    public ResponseEntity<Object> get(AuthenticatedUser user, @PathVariable UUID proposalId) {
        return results.ok(queryService.getProposal(user.id(), proposalId), MatchmakingResourceAssembler::toResource);
    }

    @PostMapping
    @Authorize(Roles.CARRIER)
    @Operation(summary = "Offer a rate to carry a shipment on one of my routes")
    public ResponseEntity<Object> create(AuthenticatedUser user, @RequestBody CreateMatchProposalResource resource) {
        return results.created(commandService.handle(new CreateMatchProposalCommand(user.id(),
                        resource.shipmentRequestId(), resource.returnRouteId(), resource.offeredRate(), resource.currency())),
                MatchmakingResourceAssembler::toResource);
    }

    @PostMapping("/{proposalId}/accept")
    @Authorize(Roles.MERCHANT)
    @Operation(summary = "Accept an offer (opens the trip)")
    public ResponseEntity<Object> accept(AuthenticatedUser user, @PathVariable UUID proposalId) {
        return results.ok(commandService.handle(new AcceptMatchProposalCommand(user.id(), proposalId)),
                MatchmakingResourceAssembler::toResource);
    }

    @PostMapping("/{proposalId}/reject")
    @Authorize(Roles.MERCHANT)
    @Operation(summary = "Reject an offer")
    public ResponseEntity<Object> reject(AuthenticatedUser user, @PathVariable UUID proposalId) {
        return results.ok(commandService.handle(new RejectMatchProposalCommand(user.id(), proposalId)),
                MatchmakingResourceAssembler::toResource);
    }

    @PostMapping("/{proposalId}/cancel")
    @Authorize(Roles.CARRIER)
    @Operation(summary = "Withdraw a pending offer")
    public ResponseEntity<Object> cancel(AuthenticatedUser user, @PathVariable UUID proposalId) {
        return results.ok(commandService.handle(new CancelMatchProposalCommand(user.id(), proposalId)),
                MatchmakingResourceAssembler::toResource);
    }

    @GetMapping("/{proposalId}/messages")
    @Operation(summary = "Read the chat of a proposal")
    public ResponseEntity<Object> listMessages(AuthenticatedUser user, @PathVariable UUID proposalId) {
        return results.ok(queryService.listMessages(user.id(), proposalId),
                messages -> messages.stream().map(MatchmakingResourceAssembler::toResource).toList());
    }

    @PostMapping("/{proposalId}/messages")
    @Operation(summary = "Send a chat message")
    public ResponseEntity<Object> sendMessage(AuthenticatedUser user, @PathVariable UUID proposalId,
                                              @RequestBody SendMessageResource resource) {
        return results.created(commandService.handle(new SendMessageCommand(user.id(), proposalId, resource.content())),
                MatchmakingResourceAssembler::toResource);
    }
}
