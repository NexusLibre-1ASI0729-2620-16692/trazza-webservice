package com.trazza.iam.infrastructure.pipeline;

import com.trazza.iam.application.internal.outboundservices.TokenService;
import com.trazza.iam.domain.model.aggregates.User;
import com.trazza.iam.domain.repositories.IamRepositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads Authorization: Bearer <token>, validates the JWT and stores the active user in
 * HttpServletRequest attributes. The filter decides whether a user is required.
 */
@Component
public class RequestAuthorizationMiddleware extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public RequestAuthorizationMiddleware(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            Optional<UUID> publicId = tokenService.validateAccessToken(token);

            if (publicId.isPresent()) {
                Optional<User> user = userRepository.findByPublicId(publicId.get());

                // Validar si el usuario puede iniciar sesión (CanSignIn)
                if (user.isPresent() && user.get().canSignIn()) {
                    AuthenticatedUser authUser = new AuthenticatedUser(
                            user.get().getId(),
                            user.get().getPublicId(),
                            user.get().getRoles().stream().map(role -> role.getRole()).collect(java.util.stream.Collectors.toSet())
                    );

                    // Guardar en los atributos del request (Equivalente a context.Items)
                    request.setAttribute(
                            AuthenticatedUser.HttpContextAuthenticationExtensions.ITEM_KEY,
                            authUser
                    );
                }
            }
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}