package com.zetcom.mpria.config.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The configured shape of one record type at one revision.
 *
 * <p>This is the definition {@code catalog-service} compiles into the validator that guards its
 * {@code jsonb} payload column, which is why an internally inconsistent definition must be refused
 * here rather than discovered there.
 *
 * @param schemaVersion increments with each published change to this record type, and is stamped
 *                      onto every record written under it so old payloads stay readable
 * @param titleFieldId  which field supplies the denormalised record title; {@code null} when the
 *                      record type has no natural title
 */
public record RecordTypeDefinition(String recordType,
                                   int schemaVersion,
                                   String titleFieldId,
                                   List<FieldDefinition> fields) {

    public RecordTypeDefinition {
        if (recordType == null || recordType.isBlank()) {
            throw new IllegalArgumentException("recordType must not be blank");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException(
                    "record type '" + recordType + "' has a schemaVersion below 1");
        }
        fields = fields == null ? List.of() : List.copyOf(fields);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("record type '" + recordType + "' declares no fields");
        }
        requireDistinctIds(recordType, fields);
        if (titleFieldId != null && fields.stream().noneMatch(f -> f.id().equals(titleFieldId))) {
            throw new IllegalArgumentException("titleFieldId '" + titleFieldId
                    + "' is not a field of record type '" + recordType + "'");
        }
    }

    /** Field ids in configured order, which is also the order the palette offers them. */
    public List<String> fieldIds() {
        return fields.stream().map(FieldDefinition::id).toList();
    }

    public Optional<FieldDefinition> field(String fieldId) {
        return fields.stream().filter(f -> f.id().equals(fieldId)).findFirst();
    }

    private static void requireDistinctIds(String recordType, List<FieldDefinition> fields) {
        Set<String> seen = new HashSet<>();
        for (FieldDefinition field : fields) {
            if (!seen.add(field.id())) {
                throw new IllegalArgumentException("record type '" + recordType
                        + "' has a duplicate field id '" + field.id() + "'");
            }
        }
    }
}
