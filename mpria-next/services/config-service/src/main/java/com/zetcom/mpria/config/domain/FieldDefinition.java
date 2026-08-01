package com.zetcom.mpria.config.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One configured field of a record type.
 *
 * @param id            stable identifier, and the key under which the value is stored in a record
 * @param label         an i18n key, never a literal caption — captions are resolved by the client
 * @param required      whether a value must be present
 * @param maxLength     for {@code STRING} and {@code TEXT}; {@code null} means unconstrained
 * @param allowedValues for {@code ENUM}, in the order a client should offer them; empty otherwise.
 *                      A list rather than a set because the order is itself a configuration
 *                      decision — it is the order the options appear in a drop-down.
 */
public record FieldDefinition(String id,
                              FieldType type,
                              String label,
                              boolean required,
                              Integer maxLength,
                              List<String> allowedValues) {

    public FieldDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("field id must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("field '" + id + "' has no type");
        }
        label = (label == null || label.isBlank()) ? "config.field." + id : label;
        allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
        requireDistinct(id, allowedValues);
        if (type == FieldType.ENUM && allowedValues.isEmpty()) {
            throw new IllegalArgumentException("enum field '" + id + "' declares no allowed values");
        }
        if (type != FieldType.ENUM && !allowedValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "field '" + id + "' is " + type + ": only an ENUM field may declare allowed values");
        }
        if (maxLength != null && maxLength < 1) {
            throw new IllegalArgumentException("field '" + id + "' has a maxLength below 1");
        }
    }

    private static void requireDistinct(String id, List<String> allowedValues) {
        Set<String> seen = new HashSet<>();
        for (String value : allowedValues) {
            if (!seen.add(value)) {
                throw new IllegalArgumentException(
                        "field '" + id + "' has a duplicate allowed value '" + value + "'");
            }
        }
    }
}
