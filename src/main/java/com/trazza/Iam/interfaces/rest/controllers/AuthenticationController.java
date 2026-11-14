package com.trazza.iam.interfaces.rest.controllers;

import com.trazza.iam.domain.model.commands.IamCommands.*;
import com.trazza.iam.domain.services.IamServices.UserCommandService;
import com.trazza.iam.infrastructure.pipeline.AuthenticatedUser.HttpContextAuthenticationExtensions;
import com.trazza.iam.interfaces.rest.resources.*;
import com.trazza.iam.interfaces.rest.transform.IamResourceAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Own authentication: sign-up, sign-in, refresh token rotation and sign-out.
 */
@RestController
@RequestMapping("/api/v1/authentication")
public class AuthenticationController {

    private final UserCommandService userCommandService;

    public AuthenticationController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody SignUpResource resource) {
        var commandResult = IamResourceAssembler.toCommand(resource);
        if (commandResult.isFailure()) {
            return ResponseEntity.badRequest().body(commandResult.getError());
        }

        return userCommandService.handle(commandResult.getValue())
                .thenApply(result -> result.isSuccess()
                        ? ResponseEntity.status(201).body(IamResourceAssembler.toResource(result.getValue()))
                        : ResponseEntity.badRequest().body(result.getError()))
                .join(); // .join() para mantener la respuesta síncrona en el controlador
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody SignInResource resource) {
        return userCommandService.handle(new SignInCommand(resource.email(), resource.password()))
                .thenApply(result -> result.isSuccess()
                        ? ResponseEntity.ok(IamResourceAssembler.toResource(result.getValue()))
                        : ResponseEntity.badRequest().body(result.getError()))
                .join();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshSessionResource resource) {
        return userCommandService.handle(new RefreshSessionCommand(resource.refreshToken()))
                .thenApply(result -> result.isSuccess()
                        ? ResponseEntity.ok(IamResourceAssembler.toResource(result.getValue()))
                        : ResponseEntity.badRequest().body(result.getError()))
                .join();
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(@RequestBody RefreshSessionResource resource) {
        var request = HttpContextAuthenticationExtensions.getCurrentRequest();
        var user = HttpContextAuthenticationExtensions.getAuthenticatedUser(request);

        return userCommandService.handle(new SignOutCommand(user.id(), resource.refreshToken()))
                .thenApply(result -> result.isSuccess()
                        ? ResponseEntity.noContent().build()
                        : ResponseEntity.badRequest().body(result.getError()))
                .join();
    }
}