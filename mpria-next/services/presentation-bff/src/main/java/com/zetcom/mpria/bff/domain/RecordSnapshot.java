package com.zetcom.mpria.bff.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A record as this service saw it at one moment, owned by catalog-service.
 *
 * <p>"Snapshot" rather than "record" on purpose: nothing here may treat it as an aggregate. It is
 * read, rendered and discarded, and the {@code etag} is the only way a later write can prove it was
 * built on this version rather than an older one.
 *
 * @param data  the configured payload, keyed by field id. Values are whatever JSON carried
 * @param etag  the optimistic-concurrency token, quoted as the HTTP header carries it
 */
public record RecordSnapshot(UUID id,
                             String recordType,
                             int schemaVersion,
                             String identifier,
                             String title,
                             Lifecycle lifecycle,
                             Map<String, Object> data,
                             String etag) {

    public RecordSnapshot {
        if (id == null) {
            throw new IllegalArgumentException("a record snapshot needs an id");
        }
        if (lifecycle == null) {
            throw new IllegalArgumentException("record " + id + " has no lifecycle");
        }
        // Not Map.copyOf: it rejects null values, and a field the user cleared arrives as null.
        data = data == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }
}
