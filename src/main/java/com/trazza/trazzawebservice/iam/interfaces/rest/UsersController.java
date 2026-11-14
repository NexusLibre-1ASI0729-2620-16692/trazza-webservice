package com.trazza.trazzawebservice.iam.interfaces.rest;

import com.trazza.trazzawebservice.iam.domain.model.commands.SubmitIdentityVerificationCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateIdentityCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateMerchantProfileCommand;
import com.trazza.trazzawebservice.iam.domain.services.UserCommandService;
import com.trazza.trazzawebservice.iam.domain.services.UserQueryService;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.iam.interfaces.rest.resources.AddRoleResource;
import com.trazza.trazzawebservice.iam.interfaces.rest.resources.SubmitIdentityVerificationResource;
import com.trazza.trazzawebservice.iam.interfaces.rest.resources.UpdateIdentityResource;
import com.trazza.trazzawebservice.iam.interfaces.rest.resources.UpdateMerchantProfileResource;
import com.trazza.trazzawebservice.iam.interfaces.rest.transform.IamResourceAssembler;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.interfaces.rest.RestResults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in account: identity, roles, merchant profile and KYC (simulated RENIEC). */
@RestController
@RequestMapping(value = "/api/v1/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize
@Tag(name = "Users", description = "The signed-in account")
public class UsersController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final RestResults results;

    public UsersController(UserCommandService userCommandService, UserQueryService userQueryService,
                           RestResults results) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
        this.results = results;
    }

    @GetMapping
    @Operation(summary = "Get my account")
    public ResponseEntity<Object> getMe(AuthenticatedUser user) {
        return userQueryService.getAccount(user.id())
                .<ResponseEntity<Object>>map(account -> ResponseEntity.ok(IamResourceAssembler.toResource(account)))
                .orElseGet(() -> results.error(CommandError.notFound("user-not-found")));
    }

    @PutMapping("/identity")
    @Operation(summary = "Update my full name and phone")
    public ResponseEntity<Object> updateIdentity(AuthenticatedUser user, @RequestBody UpdateIdentityResource resource) {
        return results.ok(userCommandService.handle(
                new UpdateIdentityCommand(user.id(), resource.fullName(), resource.phone())),
                IamResourceAssembler::toResource);
    }

    @PostMapping("/roles")
    @Operation(summary = "Add the carrier or merchant role to my account")
    public ResponseEntity<Object> addRole(AuthenticatedUser user, @RequestBody AddRoleResource resource) {
        var command = IamResourceAssembler.toCommand(user.id(), resource);
        if (command.isFailure()) return results.error(command.error());
        return results.ok(userCommandService.handle(command.value()), IamResourceAssembler::toResource);
    }

    @PutMapping("/merchant-profile")
    @Operation(summary = "Update my business name and type")
    public ResponseEntity<Object> updateMerchantProfile(AuthenticatedUser user,
                                                        @RequestBody UpdateMerchantProfileResource resource) {
        return results.ok(userCommandService.handle(
                new UpdateMerchantProfileCommand(user.id(), resource.businessName(), resource.businessType())),
                IamResourceAssembler::toResource);
    }

    @GetMapping("/identity-verifications")
    @Operation(summary = "List my identity verifications")
    public ResponseEntity<Object> listIdentityVerifications(AuthenticatedUser user) {
        return ResponseEntity.ok(userQueryService.listVerifications(user.id()).stream()
                .map(IamResourceAssembler::toResource).toList());
    }

    @PostMapping("/identity-verifications")
    @Operation(summary = "Validate my identity (simulated RENIEC)")
    public ResponseEntity<Object> submitIdentityVerification(AuthenticatedUser user,
                                                             @RequestBody SubmitIdentityVerificationResource resource) {
        return results.created(userCommandService.handle(new SubmitIdentityVerificationCommand(user.id(),
                resource.documentFrontS3Key(), resource.documentBackS3Key())), IamResourceAssembler::toResource);
    }
}
