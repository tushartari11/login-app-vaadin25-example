package com.zetcom.mpria.config.infrastructure.seed;

import java.io.IOException;
import java.io.InputStream;

import com.zetcom.mpria.config.application.ConfigurationBundle;
import com.zetcom.mpria.config.domain.ConfigurationRevision;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the published revision the service starts from.
 *
 * <p><strong>Deliberate skeleton scope.</strong> There is no database. The configuration ships as
 * a classpath resource in the export bundle format, so the same file could be produced by
 * {@code GET /api/v1/admin/revisions/{id}/export} and re-imported. Phase 2 replaces this with
 * PostgreSQL and Flyway.
 *
 * <p>A malformed seed fails the context, and therefore the container's startup probe, rather than
 * leaving a service running with no configuration to serve. That is the right failure: a
 * configuration service with an empty configuration is worse than one that is plainly down.
 */
@Component
public class SeedRevisionReader {

    public static final String SEED_RESOURCE = "config/seed-revision.json";

    private final JsonMapper json;

    public SeedRevisionReader(JsonMapper json) {
        this.json = json;
    }

    public ConfigurationRevision read() {
        try (InputStream source = new ClassPathResource(SEED_RESOURCE).getInputStream()) {
            return json.readValue(source, ConfigurationBundle.class).toPublishedRevision();
        } catch (IOException e) {
            throw new IllegalStateException("could not read the seed revision from " + SEED_RESOURCE, e);
        }
    }
}
