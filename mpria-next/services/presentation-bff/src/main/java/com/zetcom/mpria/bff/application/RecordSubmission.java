package com.zetcom.mpria.bff.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a submitted form carries.
 *
 * @param data  values keyed by field id, exactly as the client sent them. Nothing is coerced here:
 *              catalog-service validates against the same configured schema, and a BFF that
 *              silently repaired a value would make the two disagree
 * @param etag  the version the form was rendered from, relayed as {@code If-Match}. {@code null}
 *              only for a create, where there is no prior version to guard
 */
public record RecordSubmission(String identifier, Map<String, Object> data, String etag) {

    public RecordSubmission {
        // Not Map.copyOf: a field the user cleared arrives as an explicit null and must survive.
        data = data == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }
}
