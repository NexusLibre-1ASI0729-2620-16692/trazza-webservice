package com.trazza.iam.interfaces.rest.controllers;

import com.trazza.iam.domain.model.commands.IamCommands.*;
import com.trazza.iam.domain.services.IamServices.UserCommandService;
import com.trazza.iam.domain.services.IamServices.UserQueryService;
import com.trazza.iam.infrastructure.pipeline.AuthenticatedUser.HttpContextAuthenticationExtensions;
import com.trazza.iam.interfaces.rest.resources.*;
import com.trazza.iam.interfaces.rest.transform.IamResourceAssembler;
import com.trazza.shared.domain.model.CommandError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The signed-in account: identity, roles, merchant profile and KYC (simulated RENIEC).
 */
@RestController
@RequestMapping("/api/v1/users/me")
public class UsersController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public UsersController(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    private Long userId() {
        var request = HttpContextAuthenticationExtensions.getCurrentRequest();
        return HttpContextAuthenticationExtensions.getAuthenticatedUser(request).id();
    }

    @GetMapping
    public ResponseEntity<?> getMe() {
        return userQueryService.getAccount(userId())
                .thenApply(accountOpt -> accountOpt
                        .map(account -> ResponseEntity.ok(IamResourceAssembler.toResource(account)))
                        .orElseGet(() -> ResponseEntity.badRequest().body(CommandError.notFound("user-not-found"))))
                .join();
    }

    @PutMapping("/identity")
    public ResponseEntity<?> updateIdentity(@RequestBody UpdateIdentityResource resource) {
        return userCommandService.handle(new UpdateIdentityCommand(userId(), resource.fullName(), resource.phone()))
                .thenApply(result -> result.isSuccess()
                        ? ResponseEntity.ok(IamResourceAssembler.toResource(result.getValue()))
                        : ResponseEntity.badRequest().body(result.getError()))
                .join();
    }

    @PostMapping("/roles")
    public ResponseEntity<?> addRole(@RequestBody AddRoleResource resource) {
        var commandResult = IamResourceAssembler.toCommand(userId(), resource);
        if (commandResult.isFailure()) {
            return ResponseEntity.badRequest().body(commandResult.getError());
        }

        return userCommandService.handle(commandResult.getValue())
                .thenApply(result -> result.isSuccess()
                        ? ResponseEntity.ok(IamResourceAssembler.toResource(result.getValue()))
                        : ResponseEntity.badRequest().body(result.getError()))
                .join();
    }

    @PutMapping("/merchant-profile")
    public ResponseEntity<?> updateMerchantProfile(@RequestBody UpdateMerchantProfileResource resource) {
        return userCommandService.handle(new UpdateMerchantProfileCommand(userId(), resource.businessName(), resource.businessType()))
                .thenApply(result -> result.isSuccess()
                        ? ResponseEntity.ok(IamResourceAssembler.toResource(result.getValue()))
                        : ResponseEntity.badRequest().body(result.getError()))
                .join();
    }

    @GetMapping("/identity-verifications")
    public ResponseEntity<?> listIdentityVerifications() {
        return userQueryService.listVerifications(userId())
                .thenApply(verifications -> ResponseEntity.ok(verifications.stream().map(IamResourceAssembler::toResource).toList()))
                .join();
    }

    @PostMapping("/identity-verifications")
    public ResponseEntity<?> submitIdentityVerification(@RequestBody SubmitIdentityVerificationResource resource) {
        return userCommandService.handle(new SubmitIdentityVerificationCommand(userId(),
                        resource.documentFrontS3Key(), resource.documentBackS3Key()))
                .thenApply(result -> result.isSuccess()
                        ? ResponseEntity.status(201).body(IamResourceAssembler.toResource(result.getValue()))
                        : ResponseEntity.badRequest().body(result.getError()))
                .join();
    }
}