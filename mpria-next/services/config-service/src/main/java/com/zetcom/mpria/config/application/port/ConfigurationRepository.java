package com.zetcom.mpria.config.application.port;

import java.util.Optional;

import com.zetcom.mpria.config.domain.ConfigurationRevision;

/**
 * Where revisions live.
 *
 * <p>A port rather than a class so the skeleton's in-memory store can be replaced by the
 * PostgreSQL schema of Phase 2 without a line of the application layer changing.
 */
public interface ConfigurationRepository {

    /** The revision every consumer resolves and caches. Empty only before the first publication. */
    Optional<ConfigurationRevision> currentPublished();

    Optional<ConfigurationRevision> findById(String revisionId);

    ConfigurationRevision save(ConfigurationRevision revision);

    /**
     * Stores {@code revision} and points {@link #currentPublished()} at it.
     *
     * <p>Separate from {@link #save} because publication is a pointer move, which is also what
     * makes rollback publishing an earlier revision rather than restoring one.
     */
    void publish(ConfigurationRevision revision);
}
