package com.trazza.trazzawebservice.shared.domain.model;

import java.util.function.Function;

/**
 * Outcome of a domain or application operation: either a value or a {@link CommandError}, never both.
 *
 * @param <T> type of the value returned on success
 */
public final class Result<T> {

    private final T value;
    private final CommandError error;

    private Result(T value, CommandError error) {
        this.value = value;
        this.error = error;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> failure(CommandError error) {
        return new Result<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    public T value() {
        return value;
    }

    public CommandError error() {
        return error;
    }

    /** Transforms the value of a success; a failure is passed along unchanged. */
    public <R> Result<R> map(Function<T, R> mapper) {
        return isSuccess() ? success(mapper.apply(value)) : failure(error);
    }

    /** Re-types a failure so it can be returned from a method with another value type. */
    public <R> Result<R> asFailure() {
        if (isSuccess()) throw new IllegalStateException("The result is a success.");
        return failure(error);
    }
}
