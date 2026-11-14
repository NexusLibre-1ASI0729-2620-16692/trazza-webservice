package com.trazza.trazzawebservice.shared.interfaces.rest;

import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import com.trazza.trazzawebservice.shared.interfaces.rest.transform.ProblemDetailsAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Turns a {@link Result} into an HTTP response: 200/201/204 on success, a localized ProblemDetails otherwise.
 */
@Component
public class RestResults {

    private final ProblemDetailsAssembler problemDetailsAssembler;

    public RestResults(ProblemDetailsAssembler problemDetailsAssembler) {
        this.problemDetailsAssembler = problemDetailsAssembler;
    }

    public ResponseEntity<Object> error(CommandError error) {
        return problemDetailsAssembler.toResponse(error);
    }

    public <T, R> ResponseEntity<Object> ok(Result<T> result, Function<T, R> toResource) {
        return result.isSuccess() ? ResponseEntity.ok(toResource.apply(result.value())) : error(result.error());
    }

    public <T, R> ResponseEntity<Object> created(Result<T> result, Function<T, R> toResource) {
        return result.isSuccess()
                ? ResponseEntity.status(HttpStatus.CREATED).body(toResource.apply(result.value()))
                : error(result.error());
    }

    public <T> ResponseEntity<Object> noContent(Result<T> result) {
        return result.isSuccess() ? ResponseEntity.noContent().build() : error(result.error());
    }
}
