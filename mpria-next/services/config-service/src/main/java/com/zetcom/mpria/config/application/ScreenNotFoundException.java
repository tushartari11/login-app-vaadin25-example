package com.zetcom.mpria.config.application;

/** The published revision configures no such screen. */
public class ScreenNotFoundException extends RuntimeException {

    public ScreenNotFoundException(String screenId) {
        super("screen '" + screenId + "' is not configured in the current published revision");
    }
}
