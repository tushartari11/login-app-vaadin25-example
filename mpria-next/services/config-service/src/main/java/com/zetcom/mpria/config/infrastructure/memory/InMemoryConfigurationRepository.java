package com.zetcom.mpria.config.infrastructure.memory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.zetcom.mpria.config.application.port.ConfigurationRepository;
import com.zetcom.mpria.config.domain.ConfigurationRevision;

/**
 * The skeleton's configuration store: a map, seeded at startup and lost on shutdown.
 *
 * <p><strong>Deliberate skeleton scope.</strong> config-service has no database and no datasource
 * dependency. Everything it serves comes from {@code config/seed-revision.json} on the classpath,
 * and anything published or imported at runtime lives only until the process exits. Phase 2
 * replaces this class with a PostgreSQL schema owned by this service and Flyway migrations, behind
 * the same {@link ConfigurationRepository} port and with no change to the application layer.
 *
 * <p>That is a defensible position rather than a shortcut, because the service is not on the
 * runtime critical path: consumers pin to a published revision and cache it, so what matters for
 * the walking skeleton is that a correct revision is <em>served</em>, not that it survives a
 * restart.
 */
public final class InMemoryConfigurationRepository implements ConfigurationRepository {

    /**
     * The draft a cold start opens from the seeded revision.
     *
     * <p>Without it the administration API would have nothing to act on until someone imported a
     * bundle, because there is no create-draft endpoint in the Phase-0 contract.
     */
    public static final String SEED_DRAFT_ID = "draft-1";

    private final Map<String, ConfigurationRevision> revisions = new ConcurrentHashMap<>();
    private final AtomicReference<String> currentId = new AtomicReference<>();

    public InMemoryConfigurationRepository(ConfigurationRevision publishedSeed) {
        publish(publishedSeed);
        save(publishedSeed.asDraft(SEED_DRAFT_ID));
    }

    @Override
    public Optional<ConfigurationRevision> currentPublished() {
        return Optional.ofNullable(currentId.get()).map(revisions::get);
    }

    @Override
    public Optional<ConfigurationRevision> findById(String revisionId) {
        return Optional.ofNullable(revisions.get(revisionId));
    }

    @Override
    public ConfigurationRevision save(ConfigurationRevision revision) {
        revisions.put(revision.id(), revision);
        return revision;
    }

    @Override
    public void publish(ConfigurationRevision revision) {
        save(revision);
        currentId.set(revision.id());
    }
}
