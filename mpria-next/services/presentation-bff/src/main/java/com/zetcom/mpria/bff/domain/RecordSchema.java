package com.zetcom.mpria.bff.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The compiled schema of one record type in one published configuration revision.
 *
 * <p>Immutable, because the revision it came from is immutable. That is what makes it safe to hold
 * indefinitely in the configuration adapter's cache.
 *
 * @param titleFieldId which field is the record's display title. Configuration decides; nothing
 *                     here assumes a field called {@code title} exists
 */
public record RecordSchema(String recordType,
                           int schemaVersion,
                           String titleFieldId,
                           List<FieldDefinition> fields) {

    public RecordSchema {
        if (recordType == null || recordType.isBlank()) {
            throw new IllegalArgumentException("recordType must not be blank");
        }
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    /** The definition of {@code fieldId}, or empty when this record type does not define it. */
    public Optional<FieldDefinition> field(String fieldId) {
        return Optional.ofNullable(index().get(fieldId));
    }

    // Rebuilt per lookup rather than cached in a field: a schema has a handful of fields, and a
    // record component holding a derived map would break the record's equality and copy semantics.
    private Map<String, FieldDefinition> index() {
        Map<String, FieldDefinition> byId = new LinkedHashMap<>();
        fields.forEach(field -> byId.put(field.id(), field));
        return byId;
    }
}
