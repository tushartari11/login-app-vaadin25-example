package com.zetcom.mpria.config.application;

/** The published revision defines no such record type. */
public class RecordTypeNotFoundException extends RuntimeException {

    public RecordTypeNotFoundException(String recordType) {
        super("record type '" + recordType + "' is not defined in the current published revision");
    }
}
