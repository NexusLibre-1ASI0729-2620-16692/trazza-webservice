package com.trazza.trazzawebservice.shared.interfaces.rest;

import com.trazza.trazzawebservice.shared.interfaces.rest.transform.ProblemDetailsAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns framework errors into localized RFC 7807 responses. Unexpected errors never leak details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ProblemDetailsAssembler assembler;

    public GlobalExceptionHandler(ProblemDetailsAssembler assembler) {
        this.assembler = assembler;
    }

    /** Malformed JSON or a body that cannot be bound (for example an invalid date or UUID). */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Object> handleInvalidRequest(Exception exception) {
        return assembler.toResponse(HttpStatus.BAD_REQUEST, "InvalidModelStateTitle", "invalid-request");
    }

    /** A path segment that is not a UUID or a number does not match any resource. */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, NoResourceFoundException.class})
    public ResponseEntity<Object> handleNotFound(Exception exception) {
        return assembler.toResponse(HttpStatus.NOT_FOUND, "ErrorTitleNotFound", "not-found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotAllowed(Exception exception) {
        return assembler.toResponse(HttpStatus.METHOD_NOT_ALLOWED, "ErrorTitleValidation", "method-not-allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(Exception exception) {
        return assembler.toResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "InvalidModelStateTitle", "invalid-request");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception) {
        LOGGER.error("Unhandled exception", exception);
        return assembler.toResponse(HttpStatus.INTERNAL_SERVER_ERROR, "ErrorTitleUnexpected", "unexpected-error");
    }
}
