package com.zetcom.mpria.catalog.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One configured field, as published by the configuration service.
 *
 * @param id            stable identifier, the key under which the value is stored in the payload
 * @param label         i18n key; never a literal caption
 * @param required      whether a value must be present
 * @param maxLength     for {@code STRING} and {@code TEXT}; {@code null} means unconstrained
 * @param allowedValues for {@code ENUM}; empty for every other type
 */
public record FieldDefinition(String id,
                              FieldType type,
                              String label,
                              boolean required,
                              Integer maxLength,
                              Set<String> allowedValues) {

    public FieldDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("field id must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("field '" + id + "' has no type");
        }
        label = (label == null || label.isBlank()) ? "config.field." + id : label;
        allowedValues = allowedValues == null ? Set.of() : Set.copyOf(allowedValues);
        if (type == FieldType.ENUM && allowedValues.isEmpty()) {
            throw new IllegalArgumentException("enum field '" + id + "' declares no allowed values");
        }
        if (maxLength != null && maxLength < 1) {
            throw new IllegalArgumentException("field '" + id + "' has a maxLength below 1");
        }
    }

    public static FieldDefinition text(String id) {
        return new FieldDefinition(id, FieldType.STRING, null, false, null, Set.of());
    }

    public FieldDefinition asRequired() {
        return new FieldDefinition(id, type, label, true, maxLength, allowedValues);
    }

    public FieldDefinition withMaxLength(int limit) {
        return new FieldDefinition(id, type, label, required, limit, allowedValues);
    }

    /** Validation messages for {@code value}, empty when the value is acceptable. */
    List<String> violations(Object value) {
        List<String> messages = new ArrayList<>();
        if (value == null) {
            if (required) {
                messages.add("is required");
            }
            return messages;
        }
        type.reject(value).ifPresent(messages::add);
        if (!messages.isEmpty()) {
            // Length and membership checks are meaningless once the type is already wrong.
            return messages;
        }
        if (maxLength != null && value instanceof String text && text.length() > maxLength) {
            messages.add("must be at most " + maxLength + " characters");
        }
        if (type == FieldType.ENUM && !allowedValues.contains(value)) {
            messages.add("must be one of " + allowedValues.stream().sorted().toList());
        }
        return messages;
    }
}
