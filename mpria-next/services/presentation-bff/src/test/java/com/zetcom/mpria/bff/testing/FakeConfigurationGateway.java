package com.zetcom.mpria.bff.testing;

import java.util.HashMap;
import java.util.Map;

import com.zetcom.mpria.bff.application.port.ConfigurationGateway;
import com.zetcom.mpria.bff.domain.ConfigurationRevision;
import com.zetcom.mpria.bff.domain.RecordSchema;
import com.zetcom.mpria.bff.domain.RecordTypeNotConfiguredException;
import com.zetcom.mpria.bff.domain.ScreenDefinition;
import com.zetcom.mpria.bff.domain.ScreenNotFoundException;

/**
 * An in-memory configuration service.
 *
 * <p>A fake rather than a mock: it enforces the same contract the real gateway does — an unknown
 * screen or record type is absent, not null — so a test cannot accidentally assert against
 * behaviour config-service would never produce (ARCHITECTURE.md §14).
 */
public final class FakeConfigurationGateway implements ConfigurationGateway {

    private final Map<String, ScreenDefinition> screens = new HashMap<>();
    private final Map<String, RecordSchema> schemas = new HashMap<>();

    private ConfigurationRevision revision = new ConfigurationRevision(ScreenFixtures.REVISION_ID);

    public FakeConfigurationGateway with(ScreenDefinition screen) {
        screens.put(screen.screenId(), screen);
        return this;
    }

    public FakeConfigurationGateway with(RecordSchema schema) {
        schemas.put(schema.recordType(), schema);
        return this;
    }

    public FakeConfigurationGateway at(String revisionId) {
        this.revision = new ConfigurationRevision(revisionId);
        return this;
    }

    @Override
    public ConfigurationRevision currentRevision() {
        return revision;
    }

    @Override
    public ScreenDefinition screen(ConfigurationRevision pinned, String screenId) {
        ScreenDefinition screen = screens.get(screenId);
        if (screen == null) {
            throw new ScreenNotFoundException(screenId, pinned.revisionId());
        }
        return screen;
    }

    @Override
    public RecordSchema schema(ConfigurationRevision pinned, String recordType) {
        RecordSchema schema = schemas.get(recordType);
        if (schema == null) {
            throw new RecordTypeNotConfiguredException(recordType, pinned.revisionId());
        }
        return schema;
    }
}
