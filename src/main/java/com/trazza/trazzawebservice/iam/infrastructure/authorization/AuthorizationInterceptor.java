package com.trazza.trazzawebservice.iam.infrastructure.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.interfaces.rest.transform.ProblemDetailsAssembler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;

/**
 * Enforces {@link Authorize} and {@link AllowAnonymous} on the controllers of every bounded context.
 */
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private final ProblemDetailsAssembler problemDetailsAssembler;
    private final ObjectMapper objectMapper;

    public AuthorizationInterceptor(ProblemDetailsAssembler problemDetailsAssembler, ObjectMapper objectMapper) {
        this.problemDetailsAssembler = problemDetailsAssembler;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod method)) return true;
        var type = method.getBeanType();
        if (method.hasMethodAnnotation(AllowAnonymous.class) || type.isAnnotationPresent(AllowAnonymous.class))
            return true;

        var onType = type.getAnnotation(Authorize.class);
        var onMethod = method.getMethodAnnotation(Authorize.class);
        if (onType == null && onMethod == null) return true;

        var user = (AuthenticatedUser) request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE);
        if (user == null) return reject(response, CommandError.unauthorized("authentication-required"));
        if (!hasAnyRole(user, onType) || !hasAnyRole(user, onMethod))
            return reject(response, CommandError.forbidden("role-required"));
        return true;
    }

    private static boolean hasAnyRole(AuthenticatedUser user, Authorize authorize) {
        if (authorize == null || authorize.value().length == 0) return true;
        return Arrays.stream(authorize.value())
                .anyMatch(role -> user.roles().stream().anyMatch(userRole -> userRole.name().equals(role)));
    }

    private boolean reject(HttpServletResponse response, CommandError error) throws IOException {
        var entity = problemDetailsAssembler.toResponse(error);
        response.setStatus(entity.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), entity.getBody());
        return false;
    }
}
