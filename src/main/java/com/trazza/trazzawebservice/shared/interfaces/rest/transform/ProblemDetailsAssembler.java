package com.trazza.trazzawebservice.shared.interfaces.rest.transform;

import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Converts a {@link CommandError} into a localized RFC 7807 response with its stable {@code code}.
 */
@Component
public class ProblemDetailsAssembler {

    private final MessageSource messageSource;

    public ProblemDetailsAssembler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public ResponseEntity<Object> toResponse(CommandError error) {
        var status = switch (error.kind()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
        var titleKey = switch (error.kind()) {
            case VALIDATION -> "ErrorTitleValidation";
            case UNAUTHORIZED -> "ErrorTitleUnauthorized";
            case FORBIDDEN -> "ErrorTitleForbidden";
            case NOT_FOUND -> "ErrorTitleNotFound";
            case CONFLICT -> "ErrorTitleConflict";
        };
        return toResponse(status, titleKey, error.code());
    }

    public ResponseEntity<Object> toResponse(HttpStatus status, String titleKey, String code) {
        var problem = ProblemDetail.forStatus(status);
        problem.setTitle(message(titleKey));
        problem.setDetail(message(code));
        problem.setProperty("code", code);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    public String message(String key) {
        return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }
}
