package com.zetcom.mpria.catalog.application;

/** Thrown when no published configuration revision defines the requested record type. */
public class UnknownRecordTypeException extends RuntimeException {

    public UnknownRecordTypeException(String recordType) {
        super("no published schema for record type '" + recordType + "'");
    }
}
