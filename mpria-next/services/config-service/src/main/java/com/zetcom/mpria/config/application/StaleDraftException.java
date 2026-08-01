package com.zetcom.mpria.config.application;

/**
 * The write was built against a copy that has since moved on.
 *
 * <p>The designer autosaves every few seconds, so without this check two administrators editing
 * one screen would silently overwrite each other's work.
 */
public class StaleDraftException extends RuntimeException {

    public StaleDraftException(String draftId, String expected, String actual) {
        super("draft '" + draftId + "' was expected at " + expected + " but is at " + actual);
    }
}
