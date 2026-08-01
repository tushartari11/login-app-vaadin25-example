package com.zetcom.mpria.catalog.domain;

import java.util.List;
import java.util.Map;

/** Thrown when a payload does not satisfy its record type's configured schema. */
public class RecordValidationException extends RuntimeException {

    private final transient Map<String, List<String>> errors;

    public RecordValidationException(String recordType, Map<String, List<String>> errors) {
        super("record of type '" + recordType + "' is invalid: " + errors);
        this.errors = Map.copyOf(errors);
    }

    /** Field id → validation messages. */
    public Map<String, List<String>> errors() {
        return errors;
    }
}
