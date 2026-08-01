package com.zetcom.mpria.pm;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A hypermedia action the client may offer.
 *
 * <p>The client never constructs a URL and never decides whether an operation is permitted:
 * {@code href}, {@code method} and {@code enabled} are all computed server-side from the record's
 * lifecycle and the principal's permissions. A disabled action carries a {@code disabledReason} so
 * the UI can explain itself instead of showing an inert button.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActionModel(String id,
                          String label,
                          String method,
                          String href,
                          boolean enabled,
                          String disabledReason,
                          boolean confirm) {

    public ActionModel {
        id = Contract.requireId(id, "action id");
        method = Contract.requireId(method, "action method");
        href = Contract.requireId(href, "action href");
        if (!enabled && (disabledReason == null || disabledReason.isBlank())) {
            throw new IllegalArgumentException("disabled action '" + id + "' must state a reason");
        }
    }

    public static ActionModel enabled(String id, String label, String method, String href) {
        return new ActionModel(id, label, method, href, true, null, false);
    }

    public static ActionModel disabled(String id, String label, String method, String href, String reason) {
        return new ActionModel(id, label, method, href, false, reason, false);
    }

    public ActionModel requiringConfirmation() {
        return new ActionModel(id, label, method, href, enabled, disabledReason, true);
    }
}
