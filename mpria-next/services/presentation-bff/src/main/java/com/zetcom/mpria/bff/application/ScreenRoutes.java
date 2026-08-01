package com.zetcom.mpria.bff.application;

import java.util.UUID;

import com.zetcom.mpria.bff.domain.Lifecycle;

/**
 * The only place a client-facing URL is built.
 *
 * <p>The client never constructs a URL — it follows {@code actions[].href} (docs/CONTRACTS.md). For
 * that to hold, the server has to be the single author of its own URL space, so both the controller's
 * request mapping and every emitted href come from here. Two independent copies of "/api/v1/screens"
 * is precisely how a renamed route ships a form whose save button 404s.
 */
public final class ScreenRoutes {

    /** Root of the screen API. A compile-time constant so the controller can map on it directly. */
    public static final String SCREENS = "/api/v1/screens";

    private ScreenRoutes() {
    }

    /** Where a new record of this screen's type is created. */
    public static String screen(String screenId) {
        return SCREENS + "/" + screenId;
    }

    /** Where an existing record is read and written. */
    public static String record(String screenId, UUID recordId) {
        return screen(screenId) + "/" + recordId;
    }

    /**
     * Where a lifecycle transition is requested — the target of the {@code publish} and
     * {@code archive} actions.
     *
     * <p>The state is a path segment rather than a request body because it is part of the resource
     * being addressed, which is what lets the action carry the whole instruction in its href. A client
     * that can follow a URL can publish a record without knowing that publishing exists.
     */
    public static String lifecycle(String screenId, UUID recordId, Lifecycle target) {
        return record(screenId, recordId) + "/lifecycle/" + target.name();
    }
}
