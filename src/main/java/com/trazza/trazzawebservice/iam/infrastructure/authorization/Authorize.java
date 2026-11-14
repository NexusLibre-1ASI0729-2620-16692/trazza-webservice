package com.trazza.trazzawebservice.iam.infrastructure.authorization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires an authenticated user and, optionally, one of the given roles ({@code CARRIER}, {@code MERCHANT}).
 * It can be put on a controller and on its methods; both are checked.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorize {

    String[] value() default {};
}
