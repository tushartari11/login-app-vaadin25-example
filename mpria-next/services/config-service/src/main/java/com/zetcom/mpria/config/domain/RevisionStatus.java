package com.zetcom.mpria.config.domain;

/**
 * Where a revision sits in its lifecycle (docs/ui-configuration.md §2.1).
 *
 * <pre>
 *   DRAFT ──validate──▶ VALIDATED ──publish──▶ PUBLISHED
 * </pre>
 */
public enum RevisionStatus {

    /** The only mutable configuration. Nothing at runtime can see it. */
    DRAFT,

    /** A candidate that passed the publish gate and has not been edited since. */
    VALIDATED,

    /** Immutable, and the one consumers pin to and cache. */
    PUBLISHED
}
