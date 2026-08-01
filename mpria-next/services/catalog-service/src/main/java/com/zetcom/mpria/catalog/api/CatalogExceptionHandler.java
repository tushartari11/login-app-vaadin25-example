package com.zetcom.mpria.catalog.api;

import java.net.URI;

import com.zetcom.mpria.catalog.application.RecordConcurrentlyModifiedException;
import com.zetcom.mpria.catalog.application.StaleRecordException;
import com.zetcom.mpria.catalog.application.UnknownRecordTypeException;
import com.zetcom.mpria.catalog.domain.RecordNotFoundException;
import com.zetcom.mpria.catalog.domain.RecordValidationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain failures onto RFC 9457 problem documents.
 *
 * <p>Field-level validation errors are returned as structured data rather than prose, because the
 * presentation model feeds them straight back into {@code FieldState.errors} for the renderer to
 * attach to the right control.
 */
@RestControllerAdvice
class CatalogExceptionHandler {

    private static final String PROBLEM_BASE = "https://docs.mpria.zetcom.com/problems/";

    @ExceptionHandler(RecordValidationException.class)
    ProblemDetail onValidationFailure(RecordValidationException e) {
        // UNPROCESSABLE_CONTENT is the RFC 9110 name; UNPROCESSABLE_ENTITY is deprecated in Spring 7.
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT,
                "invalid-record", "The record does not satisfy its configured schema.");
        problem.setProperty("errors", e.errors());
        return problem;
    }

    @ExceptionHandler(RecordNotFoundException.class)
    ProblemDetail onNotFound(RecordNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "record-not-found", e.getMessage());
    }

    @ExceptionHandler(UnknownRecordTypeException.class)
    ProblemDetail onUnknownRecordType(UnknownRecordTypeException e) {
        return problem(HttpStatus.BAD_REQUEST, "unknown-record-type", e.getMessage());
    }

    /** Precondition Failed: the client's copy was stale before the write was even attempted. */
    @ExceptionHandler(StaleRecordException.class)
    ProblemDetail onStaleRecord(StaleRecordException e) {
        return problem(HttpStatus.PRECONDITION_FAILED, "stale-record", e.getMessage());
    }

    /** Conflict: two writers raced and the database rejected the loser. */
    @ExceptionHandler(RecordConcurrentlyModifiedException.class)
    ProblemDetail onConcurrentModification(RecordConcurrentlyModifiedException e) {
        return problem(HttpStatus.CONFLICT, "concurrent-modification", e.getMessage());
    }

    @ExceptionHandler(JwtPrincipals.MissingClaimException.class)
    ProblemDetail onMissingClaim(JwtPrincipals.MissingClaimException e) {
        return problem(HttpStatus.FORBIDDEN, "incomplete-token", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail onIllegalState(IllegalStateException e) {
        return problem(HttpStatus.CONFLICT, "illegal-transition", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE + type));
        return problem;
    }
}
