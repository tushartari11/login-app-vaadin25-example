package com.zetcom.mpria.bff.domain;

import com.zetcom.mpria.pm.Layout;

/**
 * A configured screen: which record type it edits, and how it is arranged.
 *
 * <p>The layout is the real {@code pm-contract} type rather than a parallel shape, so a layout the
 * administrator saved is the layout the renderer receives, byte for byte. Anything that reshaped it
 * in transit would be a second place for configuration semantics to live.
 */
public record ScreenDefinition(String screenId, String recordType, Layout layout) {

    public ScreenDefinition {
        if (screenId == null || screenId.isBlank()) {
            throw new IllegalArgumentException("screenId must not be blank");
        }
        if (recordType == null || recordType.isBlank()) {
            throw new IllegalArgumentException("screen '" + screenId + "' names no record type");
        }
        if (layout == null) {
            throw new IllegalArgumentException("screen '" + screenId + "' has no layout");
        }
    }
}
