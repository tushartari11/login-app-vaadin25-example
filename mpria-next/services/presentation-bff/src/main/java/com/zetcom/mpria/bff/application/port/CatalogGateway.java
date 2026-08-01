package com.zetcom.mpria.bff.application.port;

import java.util.UUID;

import com.zetcom.mpria.bff.application.RecordSubmission;
import com.zetcom.mpria.bff.domain.FieldErrors;
import com.zetcom.mpria.bff.domain.Lifecycle;
import com.zetcom.mpria.bff.domain.RecordSnapshot;

/**
 * Where record data comes from, and the one service a request may write to.
 *
 * <p>A write returns a {@link WriteResult} rather than throwing on refusal, because a refusal is an
 * ordinary outcome here: the caller submitted a form, and a form that does not validate is answered
 * with the form and its messages, not with an error. Making that a return value instead of an
 * exception is what stops the 422 path being an afterthought.
 */
public interface CatalogGateway {

    RecordSnapshot get(UUID recordId);

    WriteResult create(String recordType, RecordSubmission submission);

    WriteResult update(UUID recordId, RecordSubmission submission);

    /**
     * Moves a record to another lifecycle state.
     *
     * <p>Not a {@link WriteResult}, unlike the two methods above, and the asymmetry is deliberate: a
     * transition submits no field values, so there is nothing for schema validation to reject and no
     * form to hand back with messages on it. A refusal here means the <em>move</em> was not legal,
     * which is a genuine error.
     */
    RecordSnapshot transition(UUID recordId, Lifecycle target);

    /** Either the record as stored, or the reasons it was not. */
    sealed interface WriteResult {

        record Stored(RecordSnapshot record) implements WriteResult {
        }

        record Rejected(FieldErrors errors) implements WriteResult {
        }
    }
}
