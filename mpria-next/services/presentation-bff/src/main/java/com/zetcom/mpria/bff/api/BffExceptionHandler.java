package com.zetcom.mpria.bff.api;

import java.net.URI;

import com.zetcom.mpria.bff.application.IncompleteCallerTokenException;
import com.zetcom.mpria.bff.application.UpstreamUnavailableException;
import com.zetcom.mpria.bff.domain.IllegalLifecycleTransitionException;
import com.zetcom.mpria.bff.domain.LayoutSchemaMismatchException;
import com.zetcom.mpria.bff.domain.RecordNotFoundException;
import com.zetcom.mpria.bff.domain.RecordTypeMismatchException;
import com.zetcom.mpria.bff.domain.RecordTypeNotConfiguredException;
import com.zetcom.mpria.bff.domain.ScreenNotFoundException;
import com.zetcom.mpria.bff.domain.StaleRecordException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps failures onto RFC 9457 problem documents.
 *
 * <p>Notice what is <em>not</em> here: a handler for a validation failure. A record that does not
 * satisfy its schema is answered with a 200 and a re-rendered form carrying the messages on the right
 * controls, which is the assembler's job. An exception handler for it would mean the form had already
 * been lost.
 */
@RestControllerAdvice
class BffExceptionHandler {

    private static final String PROBLEM_BASE = "https://docs.mpria.zetcom.com/problems/";

    @ExceptionHandler(RecordNotFoundException.class)
    ProblemDetail onRecordNotFound(RecordNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "record-not-found", e.getMessage());
    }

    @ExceptionHandler(ScreenNotFoundException.class)
    ProblemDetail onScreenNotFound(ScreenNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "screen-not-found", e.getMessage());
    }

    @ExceptionHandler(RecordTypeNotConfiguredException.class)
    ProblemDetail onRecordTypeNotConfigured(RecordTypeNotConfiguredException e) {
        return problem(HttpStatus.NOT_FOUND, "unknown-record-type", e.getMessage());
    }

    @ExceptionHandler(RecordTypeMismatchException.class)
    ProblemDetail onRecordTypeMismatch(RecordTypeMismatchException e) {
        return problem(HttpStatus.BAD_REQUEST, "record-type-mismatch", e.getMessage());
    }

    /** Precondition Failed, relayed: the caller has to re-read before writing again. */
    @ExceptionHandler(StaleRecordException.class)
    ProblemDetail onStaleRecord(StaleRecordException e) {
        return problem(HttpStatus.PRECONDITION_FAILED, "stale-record", e.getMessage());
    }

    /**
     * Conflict: the move itself is not permitted from the record's current state. Distinct from
     * {@code stale-record} on purpose — the caller is not being asked to reload and retry, they are
     * being told the action does not apply.
     */
    @ExceptionHandler(IllegalLifecycleTransitionException.class)
    ProblemDetail onIllegalTransition(IllegalLifecycleTransitionException e) {
        return problem(HttpStatus.CONFLICT, "illegal-transition", e.getMessage());
    }

    @ExceptionHandler(IncompleteCallerTokenException.class)
    ProblemDetail onIncompleteToken(IncompleteCallerTokenException e) {
        return problem(HttpStatus.FORBIDDEN, "incomplete-token", e.getMessage());
    }

    /**
     * A configuration defect, and therefore a server fault rather than a caller error. Reported as
     * one so it shows up in the error rate of the deployment that can fix it: config-service should
     * have refused to publish this layout.
     */
    @ExceptionHandler(LayoutSchemaMismatchException.class)
    ProblemDetail onLayoutSchemaMismatch(LayoutSchemaMismatchException e) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "layout-schema-mismatch", e.getMessage());
    }

    /** The screen could not be assembled because a service it depends on could not answer. */
    @ExceptionHandler(UpstreamUnavailableException.class)
    ProblemDetail onUpstreamUnavailable(UpstreamUnavailableException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "upstream-unavailable", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE + type));
        return problem;
    }
}
