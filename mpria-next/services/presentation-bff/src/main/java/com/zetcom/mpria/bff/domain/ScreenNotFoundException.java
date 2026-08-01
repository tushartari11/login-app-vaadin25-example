package com.zetcom.mpria.bff.domain;

/** The published configuration revision defines no such screen. */
public class ScreenNotFoundException extends RuntimeException {

    public ScreenNotFoundException(String screenId, String revisionId) {
        super("configuration revision " + revisionId + " defines no screen '" + screenId + "'");
    }
}
