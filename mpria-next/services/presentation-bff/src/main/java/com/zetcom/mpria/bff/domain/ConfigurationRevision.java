package com.zetcom.mpria.bff.domain;

/**
 * The identity of a published configuration revision.
 *
 * <p>A wrapper around one string earns its keep twice. It cannot be confused with a screen id or a
 * record type at a call site that takes several of them, and it names the thing that pins a whole
 * request: layout and schema are both resolved <em>at</em> a revision, so a publish landing
 * mid-request cannot produce a form whose fields and arrangement disagree.
 */
public record ConfigurationRevision(String revisionId) {

    public ConfigurationRevision {
        if (revisionId == null || revisionId.isBlank()) {
            throw new IllegalArgumentException("revisionId must not be blank");
        }
    }
}
