package com.zetcom.mpria.bff.api;

import java.util.Map;

/**
 * Wire types for the screen API.
 *
 * <p>Only one shape is needed: what a submitted form carries. Everything the client receives is a
 * {@code PresentationModel} from {@code pm-contract}, returned directly rather than wrapped, because
 * a BFF-specific envelope around it would be a second contract for clients to learn.
 *
 * <p>No bean-validation annotations. The configured schema decides which fields are required and what
 * they may contain, catalog-service enforces it, and a structural check here that disagreed with the
 * configuration would reject a valid form before the rules ever ran.
 */
final class ScreenDtos {

    private ScreenDtos() {
    }

    /**
     * The fixed write body of {@code docs/CONTRACTS.md}, for both {@code PUT} and {@code POST}.
     *
     * <p>The client sends {@code values} and catalog-service stores {@code data}. The two names are
     * not an accident to be tidied away: this service is where a form's vocabulary meets a record's,
     * and renaming either end to match the other would make one of two published contracts wrong.
     *
     * @param values keyed by field id, as the renderer collected them from the form. Only fields the
     *               layout binds; anything else has no control it could have come from
     */
    record SubmissionRequest(String identifier, Map<String, Object> values) {
    }
}
