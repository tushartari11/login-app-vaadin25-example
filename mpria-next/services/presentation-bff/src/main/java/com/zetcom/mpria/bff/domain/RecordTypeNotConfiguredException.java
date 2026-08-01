package com.zetcom.mpria.bff.domain;

/** The published configuration revision defines no such record type. */
public class RecordTypeNotConfiguredException extends RuntimeException {

    public RecordTypeNotConfiguredException(String recordType, String revisionId) {
        super("configuration revision " + revisionId + " defines no record type '" + recordType + "'");
    }
}
