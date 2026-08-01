package com.zetcom.mpria.bff.domain;

import java.util.UUID;

/**
 * The write was built on a version of the record that has since moved on.
 *
 * <p>Unlike a validation failure this is <em>not</em> re-rendered as a form. Showing the caller's
 * values back with a message would invite them to submit again and overwrite whoever won the race;
 * they have to see the current record first.
 */
public class StaleRecordException extends RuntimeException {

    /** @param etag the version the write was built on, or {@code null} if it carried none */
    public StaleRecordException(UUID recordId, String etag) {
        super("record " + recordId + " has moved on since "
                + (etag == null ? "this form was rendered" : etag + " was read"));
    }
}
