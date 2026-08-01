package com.zetcom.mpria.catalog.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.zetcom.mpria.catalog.application.port.RecordSchemaProvider;
import com.zetcom.mpria.catalog.domain.RecordSchema;

/** In-memory stand-in for the configuration service. */
public final class StubRecordSchemaProvider implements RecordSchemaProvider {

    private final Map<String, RecordSchema> schemas = new HashMap<>();

    public StubRecordSchemaProvider with(RecordSchema schema) {
        schemas.put(key(schema.recordType()), schema);
        return this;
    }

    @Override
    public Optional<RecordSchema> schemaFor(UUID tenantId, String recordType) {
        return Optional.ofNullable(schemas.get(key(recordType)));
    }

    private static String key(String recordType) {
        return recordType;
    }
}
