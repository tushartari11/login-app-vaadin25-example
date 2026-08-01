package com.zetcom.mpria.catalog.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The compiled shape of one record type at one configuration revision.
 *
 * <p>This is what keeps the {@code jsonb} payload from being a schema-less free-for-all. The
 * payload column is exactly as constrained as the administrator's configuration says it is:
 * unknown keys are rejected, types are enforced, and required fields are required.
 *
 * @param schemaVersion increments with each published change, and is stamped onto every record
 *                      written under it so old payloads stay readable
 * @param titleFieldId  which field supplies the denormalised {@code title} spine column
 */
public record RecordSchema(String recordType,
                           int schemaVersion,
                           List<FieldDefinition> fields,
                           String titleFieldId) {

    public RecordSchema {
        if (recordType == null || recordType.isBlank()) {
            throw new IllegalArgumentException("recordType must not be blank");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be >= 1");
        }
        fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
        long distinct = fields.stream().map(FieldDefinition::id).distinct().count();
        if (distinct != fields.size()) {
            throw new IllegalArgumentException("duplicate field id in record type '" + recordType + "'");
        }
        if (titleFieldId != null && fields.stream().noneMatch(f -> f.id().equals(titleFieldId))) {
            throw new IllegalArgumentException(
                    "titleFieldId '" + titleFieldId + "' is not a field of '" + recordType + "'");
        }
    }

    public Optional<FieldDefinition> field(String fieldId) {
        return fields.stream().filter(f -> f.id().equals(fieldId)).findFirst();
    }

    /**
     * Validates a payload.
     *
     * <p>Errors are keyed by field id and sorted, so an API response is deterministic and a test
     * can assert on it without ordering flake.
     *
     * @return field id → messages; empty when the payload is valid
     */
    public Map<String, List<String>> validate(Map<String, Object> payload) {
        Map<String, Object> values = payload == null ? Map.of() : payload;
        Map<String, List<String>> errors = new TreeMap<>();

        for (FieldDefinition field : fields) {
            List<String> messages = field.violations(values.get(field.id()));
            if (!messages.isEmpty()) {
                errors.put(field.id(), List.copyOf(messages));
            }
        }
        for (String key : values.keySet()) {
            if (field(key).isEmpty()) {
                errors.put(key, List.of("is not a field of record type '" + recordType + "'"));
            }
        }
        return errors;
    }

    /**
     * Drops keys whose value is null and returns an insertion-ordered copy following the
     * configured field order, so stored payloads compare cleanly across writes.
     */
    Map<String, Object> normalise(Map<String, Object> payload) {
        Map<String, Object> values = payload == null ? Map.of() : payload;
        Map<String, Object> normalised = new LinkedHashMap<>();
        for (FieldDefinition field : fields) {
            Object value = values.get(field.id());
            if (value != null) {
                normalised.put(field.id(), value);
            }
        }
        return normalised;
    }

    /** The value that should be denormalised into the {@code title} spine column. */
    Optional<String> titleOf(Map<String, Object> payload) {
        if (titleFieldId == null || payload == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(payload.get(titleFieldId)).map(Object::toString);
    }
}
