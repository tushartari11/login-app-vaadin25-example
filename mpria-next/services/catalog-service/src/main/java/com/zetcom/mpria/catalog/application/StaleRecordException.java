package com.zetcom.mpria.catalog.application;

import java.util.UUID;

/** Thrown when a write is based on a copy of the record that has since been superseded. */
public class StaleRecordException extends RuntimeException {

    public StaleRecordException(UUID recordId, String expectedEtag, String actualEtag) {
        super("record " + recordId + " has moved on: expected " + expectedEtag + " but found " + actualEtag);
    }
}
