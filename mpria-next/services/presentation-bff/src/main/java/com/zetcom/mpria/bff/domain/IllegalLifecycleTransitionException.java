package com.zetcom.mpria.bff.domain;

import java.util.UUID;

/**
 * catalog-service refused the lifecycle move.
 *
 * <p>The presentation model advertises which transitions are available, but it does not decide them:
 * the state machine belongs to the service that owns the record. Reaching this normally means the
 * caller acted on a model it had held for a while and the record has moved on since, so the honest
 * answer is an error and a re-read — not a form, because no field value was in question.
 */
public class IllegalLifecycleTransitionException extends RuntimeException {

    public IllegalLifecycleTransitionException(UUID recordId, Lifecycle target) {
        super("record " + recordId + " cannot move to " + target + " from its current state");
    }
}
