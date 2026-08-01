package com.zetcom.mpria.catalog.domain;

import java.util.UUID;

/** Thrown when a record does not exist, or exists in a different tenant. */
public class RecordNotFoundException extends RuntimeException {

    public RecordNotFoundException(UUID recordId) {
        super("no record " + recordId);
    }
}
