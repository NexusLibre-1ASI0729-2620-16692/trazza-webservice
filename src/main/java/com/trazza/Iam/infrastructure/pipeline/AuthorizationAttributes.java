package com.trazza.iam.infrastructure.pipeline;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class AuthorizationAttributes {

    /**
     * Requires an authenticated user and, optionally, one of the given roles (CARRIER, MERCHANT).
     * Reemplaza el [Authorize(params string[] roles)] de C#.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("isAuthenticated()") // Lógica base: debe estar autenticado
    public @interface Authorize {
        // En Spring, los roles se validan a nivel de método usando SpEL
        // (ej. @Authorize(roles = {"CARRIER"}))
        String[] roles() default {};
    }

    /**
     * Marks an action that does not need a bearer token (sign-up, sign-in and refresh).
     * Reemplaza el [AllowAnonymous] de C#.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AllowAnonymous {
    }
}