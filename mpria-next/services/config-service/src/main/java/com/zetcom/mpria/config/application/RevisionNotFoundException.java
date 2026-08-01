package com.zetcom.mpria.config.application;

/** No such revision — or, for {@code current}, nothing has ever been published. */
public class RevisionNotFoundException extends RuntimeException {

    public RevisionNotFoundException(String revisionId) {
        super("revision '" + revisionId + "' does not exist");
    }

    public static RevisionNotFoundException nothingPublished() {
        return new RevisionNotFoundException("current");
    }
}
