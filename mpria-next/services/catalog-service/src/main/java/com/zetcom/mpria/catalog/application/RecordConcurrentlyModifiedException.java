package com.zetcom.mpria.catalog.application;

import java.util.UUID;

/**
 * Thrown when two writers raced and the database rejected the loser.
 *
 * <p>Distinct from {@link StaleRecordException}: that one is detected before the write is
 * attempted, from the client's {@code If-Match}. This one is the optimistic-lock failure that
 * surfaces at flush time, when two requests both believed they were current.
 */
public class RecordConcurrentlyModifiedException extends RuntimeException {

    public RecordConcurrentlyModifiedException(UUID recordId, Throwable cause) {
        super("record " + recordId + " was modified concurrently", cause);
    }
}
