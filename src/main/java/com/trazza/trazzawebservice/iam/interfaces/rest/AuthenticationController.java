package com.trazza.trazzawebservice.iam.interfaces.rest;

import com.trazza.trazzawebservice.iam.domain.model.commands.RefreshSessionCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.SignInCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.SignOutCommand;
import com.trazza.trazzawebservice.iam.domain.services.UserCommandService;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AllowAnonymous;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import com.trazza.trazzawebservice.iam.interfaces.rest.resources.RefreshSessionResource;
import com.trazza.trazzawebservice.iam.interfaces.rest.resources.SignInResource;
import com.trazza.trazzawebservice.iam.interfaces.rest.resources.SignUpResource;
import com.trazza.trazzawebservice.iam.interfaces.rest.transform.IamResourceAssembler;
import com.trazza.trazzawebservice.shared.interfaces.rest.RestResults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Own authentication: sign-up, sign-in, refresh token rotation and sign-out. */
@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize
@Tag(name = "Authentication", description = "Sign-up, sign-in, refresh and sign-out")
public class AuthenticationController {

    private final UserCommandService userCommandService;
    private final RestResults results;

    public AuthenticationController(UserCommandService userCommandService, RestResults results) {
        this.userCommandService = userCommandService;
        this.results = results;
    }

    @PostMapping("/sign-up")
    @AllowAnonymous
    @SecurityRequirements
    @Operation(summary = "Create an account (carrier, merchant or both) and start a session")
    public ResponseEntity<Object> signUp(@RequestBody SignUpResource resource) {
        var command = IamResourceAssembler.toCommand(resource);
        if (command.isFailure()) return results.error(command.error());
        return results.created(userCommandService.handle(command.value()), IamResourceAssembler::toResource);
    }

    @PostMapping("/sign-in")
    @AllowAnonymous
    @SecurityRequirements
    @Operation(summary = "Sign in with e-mail and password")
    public ResponseEntity<Object> signIn(@RequestBody SignInResource resource) {
        return results.ok(userCommandService.handle(new SignInCommand(resource.email(), resource.password())),
                IamResourceAssembler::toResource);
    }

    @PostMapping("/refresh")
    @AllowAnonymous
    @SecurityRequirements
    @Operation(summary = "Rotate the refresh token and get a new access token")
    public ResponseEntity<Object> refresh(@RequestBody RefreshSessionResource resource) {
        return results.ok(userCommandService.handle(new RefreshSessionCommand(resource.refreshToken())),
                IamResourceAssembler::toResource);
    }

    @PostMapping("/sign-out")
    @Operation(summary = "Revoke the refresh token of the session")
    public ResponseEntity<Object> signOut(AuthenticatedUser user, @RequestBody RefreshSessionResource resource) {
        return results.noContent(userCommandService.handle(new SignOutCommand(user.id(), resource.refreshToken())));
    }
}
