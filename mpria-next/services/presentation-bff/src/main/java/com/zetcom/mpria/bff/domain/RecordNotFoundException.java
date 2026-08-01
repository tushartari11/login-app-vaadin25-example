package com.zetcom.mpria.bff.domain;

import java.util.UUID;

/** The record the screen was asked to render does not exist, or belongs to another tenant. */
public class RecordNotFoundException extends RuntimeException {

    public RecordNotFoundException(UUID recordId) {
        super("no record " + recordId + " is visible to this caller");
    }
}
