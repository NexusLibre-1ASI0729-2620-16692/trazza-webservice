package com.trazza.trazzawebservice.shared.domain.model;

/**
 * Category of a business error; the interfaces layer maps it to an HTTP status code.
 */
public enum CommandErrorKind {
    VALIDATION,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT
}
