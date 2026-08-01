package com.zetcom.mpria.config.api;

import java.net.URI;

import com.zetcom.mpria.config.api.ConfigDtos.ConfigurationErrorResponse;
import com.zetcom.mpria.config.application.DraftNotFoundException;
import com.zetcom.mpria.config.application.RecordTypeNotFoundException;
import com.zetcom.mpria.config.application.RevisionNotFoundException;
import com.zetcom.mpria.config.application.ScreenNotFoundException;
import com.zetcom.mpria.config.application.StaleDraftException;
import com.zetcom.mpria.config.domain.ConfigurationValidationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps failures onto RFC 9457 problem documents.
 *
 * <p>Validation failures carry their errors as structured data rather than prose, because the admin
 * console selects the offending node on the canvas from them. An SME must be shown which slot to
 * fix; a stack trace tells them only that somebody else has work to do.
 */
@RestControllerAdvice
class ConfigExceptionHandler {

    private static final String PROBLEM_BASE = "https://docs.mpria.zetcom.com/problems/";

    @ExceptionHandler(ConfigurationValidationException.class)
    ProblemDetail onInvalidConfiguration(ConfigurationValidationException e) {
        // UNPROCESSABLE_CONTENT is the RFC 9110 name; UNPROCESSABLE_ENTITY is deprecated in Spring 7.
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-configuration",
                "The configuration cannot be published.");
        problem.setProperty("errors", ConfigurationErrorResponse.of(e.errors()));
        return problem;
    }

    @ExceptionHandler(RecordTypeNotFoundException.class)
    ProblemDetail onRecordTypeNotFound(RecordTypeNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "record-type-not-found", e.getMessage());
    }

    @ExceptionHandler(ScreenNotFoundException.class)
    ProblemDetail onScreenNotFound(ScreenNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "screen-not-found", e.getMessage());
    }

    @ExceptionHandler(RevisionNotFoundException.class)
    ProblemDetail onRevisionNotFound(RevisionNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "revision-not-found", e.getMessage());
    }

    @ExceptionHandler(DraftNotFoundException.class)
    ProblemDetail onDraftNotFound(DraftNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "draft-not-found", e.getMessage());
    }

    /** Precondition Failed: the designer's copy was stale before the write was even attempted. */
    @ExceptionHandler(StaleDraftException.class)
    ProblemDetail onStaleDraft(StaleDraftException e) {
        return problem(HttpStatus.PRECONDITION_FAILED, "stale-draft", e.getMessage());
    }

    /**
     * A request the domain refused to model at all — a layout naming a different screen than the
     * path does, or a bundle declaring a field type that does not exist.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onMalformedRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "malformed-configuration", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE + type));
        return problem;
    }
}
