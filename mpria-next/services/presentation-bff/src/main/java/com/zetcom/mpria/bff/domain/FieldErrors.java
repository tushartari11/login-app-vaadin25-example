package com.zetcom.mpria.bff.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validation messages from a refused write, keyed by field id.
 *
 * <p>Structured rather than prose, because the whole point is to attach each message to the control
 * that produced it. A single joined sentence would force the renderer to guess, which is how server
 * validation ends up rendered as a toast the user cannot act on.
 */
public record FieldErrors(Map<String, List<String>> byFieldId) {

    private static final FieldErrors NONE = new FieldErrors(Map.of());

    public FieldErrors {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (byFieldId != null) {
            byFieldId.forEach((fieldId, messages) -> copy.put(fieldId, List.copyOf(messages)));
        }
        byFieldId = Map.copyOf(copy);
    }

    public static FieldErrors none() {
        return NONE;
    }

    public boolean isEmpty() {
        return byFieldId.isEmpty();
    }

    /** The messages for {@code fieldId}, empty when the field was accepted. */
    public List<String> messagesFor(String fieldId) {
        return byFieldId.getOrDefault(fieldId, List.of());
    }

    public Set<String> fieldIds() {
        return byFieldId.keySet();
    }
}
