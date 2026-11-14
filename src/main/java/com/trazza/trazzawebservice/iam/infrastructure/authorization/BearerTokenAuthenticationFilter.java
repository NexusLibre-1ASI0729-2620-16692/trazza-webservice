package com.trazza.trazzawebservice.iam.infrastructure.authorization;

import com.trazza.trazzawebservice.iam.application.internal.outboundservices.tokens.TokenService;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.User;
import com.trazza.trazzawebservice.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads {@code Authorization: Bearer <token>}, validates the JWT and stores the active user as a request attribute.
 * The {@link Authorize} annotation decides whether a user is required.
 */
@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";
    private final TokenService tokenService;
    private final UserRepository userRepository;

    public BearerTokenAuthenticationFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            tokenService.validateAccessToken(header.substring(PREFIX.length()).trim())
                    .flatMap(userRepository::findByPublicId)
                    .filter(User::canSignIn)
                    .ifPresent(user -> request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE,
                            new AuthenticatedUser(user.getId(), user.getPublicId(), user.roleTypes())));
        }
        chain.doFilter(request, response);
    }
}
