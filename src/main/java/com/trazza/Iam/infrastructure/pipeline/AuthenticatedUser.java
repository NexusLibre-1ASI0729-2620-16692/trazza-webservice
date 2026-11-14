package com.trazza.iam.infrastructure.pipeline;

import com.trazza.iam.domain.model.valueobjects.IamEnums.UserRoleType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User resolved from the bearer token for the current request.
 */
public record AuthenticatedUser(
        Long id,
        UUID publicId,
        Set<UserRoleType> roles
) {
    public boolean isCarrier() {
        return roles.contains(UserRoleType.CARRIER);
    }

    public boolean isMerchant() {
        return roles.contains(UserRoleType.MERCHANT);
    }

    public static class HttpContextAuthenticationExtensions {
        public static final String ITEM_KEY = "Trazza.AuthenticatedUser";

        /**
         * User of the request; only call it from actions protected with @PreAuthorize.
         */
        public static AuthenticatedUser getAuthenticatedUser(HttpServletRequest context) {
            Object user = context.getAttribute(ITEM_KEY);
            if (user == null) {
                throw new IllegalStateException("The request has no authenticated user.");
            }
            return (AuthenticatedUser) user;
        }

        public static AuthenticatedUser findAuthenticatedUser(HttpServletRequest context) {
            return (AuthenticatedUser) context.getAttribute(ITEM_KEY);
        }

        // Helper para obtener el request actual desde cualquier parte del código
        public static HttpServletRequest getCurrentRequest() {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                throw new IllegalStateException("No web request context found.");
            }
            return attributes.getRequest();
        }
    }
}