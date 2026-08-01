package com.zetcom.mpria.bff.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zetcom.mpria.bff.application.RecordSubmission;
import com.zetcom.mpria.bff.application.port.CatalogGateway;
import com.zetcom.mpria.bff.domain.FieldErrors;
import com.zetcom.mpria.bff.domain.Lifecycle;
import com.zetcom.mpria.bff.domain.RecordNotFoundException;
import com.zetcom.mpria.bff.domain.RecordSnapshot;

/**
 * An in-memory catalogue service.
 *
 * <p>Writes either store or are rejected with field errors, exactly as the real adapter reports a
 * 201/200 and a 422. {@link #rejecting} is what lets the assembler's most important behaviour — a
 * validation failure re-rendered as a form, not as an error — be tested without an HTTP stack.
 */
public final class FakeCatalogGateway implements CatalogGateway {

    /** A transition advances the record's version, so the form's next write needs the new one. */
    public static final String ETAG_AFTER_TRANSITION = "\"8\"";

    private final Map<UUID, RecordSnapshot> records = new HashMap<>();

    private FieldErrors rejection = FieldErrors.none();
    private final List<RecordSubmission> submissions = new ArrayList<>();

    public FakeCatalogGateway with(RecordSnapshot record) {
        records.put(record.id(), record);
        return this;
    }

    /** Every subsequent write is rejected with these field errors. */
    public FakeCatalogGateway rejecting(Map<String, List<String>> errors) {
        this.rejection = new FieldErrors(errors);
        return this;
    }

    /** What the gateway was actually asked to write — used to assert the relayed ETag. */
    public List<RecordSubmission> submissions() {
        return List.copyOf(submissions);
    }

    @Override
    public RecordSnapshot get(UUID recordId) {
        RecordSnapshot record = records.get(recordId);
        if (record == null) {
            throw new RecordNotFoundException(recordId);
        }
        return record;
    }

    @Override
    public WriteResult create(String recordType, RecordSubmission submission) {
        submissions.add(submission);
        if (!rejection.isEmpty()) {
            return new WriteResult.Rejected(rejection);
        }
        RecordSnapshot created = new RecordSnapshot(
                UUID.randomUUID(), recordType, 1, submission.identifier(), submission.identifier(),
                Lifecycle.DRAFT, submission.data(), "\"0\"");
        records.put(created.id(), created);
        return new WriteResult.Stored(created);
    }

    @Override
    public WriteResult update(UUID recordId, RecordSubmission submission) {
        submissions.add(submission);
        RecordSnapshot current = get(recordId);
        if (!rejection.isEmpty()) {
            return new WriteResult.Rejected(rejection);
        }
        RecordSnapshot updated = new RecordSnapshot(current.id(), current.recordType(),
                current.schemaVersion(), submission.identifier(), current.title(),
                current.lifecycle(), submission.data(), current.etag());
        records.put(recordId, updated);
        return new WriteResult.Stored(updated);
    }

    /**
     * Moves the record, without judging whether the move was legal.
     *
     * <p>Deliberately permissive: the state machine belongs to catalog-service, so a fake that
     * enforced it here would be a second copy of a rule this service does not own.
     */
    @Override
    public RecordSnapshot transition(UUID recordId, Lifecycle target) {
        RecordSnapshot current = get(recordId);
        RecordSnapshot moved = new RecordSnapshot(current.id(), current.recordType(),
                current.schemaVersion(), current.identifier(), current.title(),
                target, current.data(), ETAG_AFTER_TRANSITION);
        records.put(recordId, moved);
        return moved;
    }
}
