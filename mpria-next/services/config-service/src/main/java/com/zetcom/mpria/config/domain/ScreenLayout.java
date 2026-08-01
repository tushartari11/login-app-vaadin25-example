package com.zetcom.mpria.config.domain;

import java.util.List;

import com.zetcom.mpria.pm.Layout;

/**
 * A configured screen: which record type it edits, and the layout tree a renderer draws.
 *
 * <p>The layout is the real {@code pm-contract} type rather than a parallel shape of this
 * service's own. Serialising the type the renderer deserialises is what stops the two drifting
 * apart one optional field at a time.
 *
 * <p>{@link Layout} carries its own {@code screenId} and {@code recordType}. Holding them here as
 * well is redundant only until the two disagree, at which point a consumer's behaviour depends on
 * which copy it happened to read — so the constructor refuses the disagreement outright.
 */
public record ScreenLayout(String screenId, String recordType, Layout layout) {

    public ScreenLayout {
        if (screenId == null || screenId.isBlank()) {
            throw new IllegalArgumentException("screenId must not be blank");
        }
        if (recordType == null || recordType.isBlank()) {
            throw new IllegalArgumentException("recordType must not be blank");
        }
        if (layout == null) {
            throw new IllegalArgumentException("screen '" + screenId + "' has no layout");
        }
        if (!screenId.equals(layout.screenId())) {
            throw new IllegalArgumentException("screen '" + screenId + "': layout declares screenId '"
                    + layout.screenId() + "'");
        }
        if (!recordType.equals(layout.recordType())) {
            throw new IllegalArgumentException("screen '" + screenId + "': layout declares recordType '"
                    + layout.recordType() + "' but the screen is configured for '" + recordType + "'");
        }
    }

    /** Every field id this screen binds, in document order. */
    public List<String> boundFieldIds() {
        return layout.boundFieldIds();
    }
}
