package com.trazza.trazzawebservice.shared.domain.model;

/**
 * Business error with a stable kebab-case code that is also the key of its localized message
 * in {@code messages*.properties}.
 *
 * @param kind category of the error
 * @param code stable error code, e.g. {@code email-already-registered}
 */
public record CommandError(CommandErrorKind kind, String code) {

    public static CommandError validation(String code) {
        return new CommandError(CommandErrorKind.VALIDATION, code);
    }

    public static CommandError unauthorized(String code) {
        return new CommandError(CommandErrorKind.UNAUTHORIZED, code);
    }

    public static CommandError forbidden(String code) {
        return new CommandError(CommandErrorKind.FORBIDDEN, code);
    }

    public static CommandError notFound(String code) {
        return new CommandError(CommandErrorKind.NOT_FOUND, code);
    }

    public static CommandError conflict(String code) {
        return new CommandError(CommandErrorKind.CONFLICT, code);
    }
}
