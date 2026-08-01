package com.zetcom.mpria.bff.domain;

/**
 * The caller asked a screen for a record type it does not edit.
 *
 * <p>Refused rather than reinterpreted. Honouring the screen's own record type would render a form
 * that looks right and writes to the wrong type; honouring the caller's would render a layout whose
 * slots no schema can satisfy.
 */
public class RecordTypeMismatchException extends RuntimeException {

    public RecordTypeMismatchException(String screenId, String configured, String requested) {
        super("screen '" + screenId + "' edits record type '" + configured
                + "', but '" + requested + "' was requested");
    }
}
