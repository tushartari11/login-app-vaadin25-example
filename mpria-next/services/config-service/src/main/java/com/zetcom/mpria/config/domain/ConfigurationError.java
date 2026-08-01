package com.zetcom.mpria.config.domain;

/**
 * One reason a configuration cannot be published.
 *
 * <p>Structured rather than prose so the admin console can select the offending node on the canvas
 * when the SME clicks the error. An SME must be told which slot to fix, not handed a stack trace.
 *
 * @param nodeId the layout node at fault, or {@code null} when the problem is with the screen as a
 *               whole rather than with anything inside it
 */
public record ConfigurationError(String screenId, String nodeId, String message) {

    public ConfigurationError {
        if (screenId == null || screenId.isBlank()) {
            throw new IllegalArgumentException("a configuration error must name the screen");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("a configuration error must carry a message");
        }
    }

    /** The one-line rendering used in logs and in the exception's own message. */
    public String describe() {
        return nodeId == null
                ? "screen '" + screenId + "' " + message
                : "screen '" + screenId + "', node '" + nodeId + "' " + message;
    }
}
