package com.zetcom.mpria.config.application;

/**
 * No editable draft under this id.
 *
 * <p>A published revision is reported the same way rather than as a permission failure: from the
 * console's point of view there is simply no draft there to edit.
 */
public class DraftNotFoundException extends RuntimeException {

    public DraftNotFoundException(String draftId) {
        super("draft '" + draftId + "' does not exist");
    }
}
