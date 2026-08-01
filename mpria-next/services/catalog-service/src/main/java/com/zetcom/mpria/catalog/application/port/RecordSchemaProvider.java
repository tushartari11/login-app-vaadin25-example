package com.zetcom.mpria.catalog.application.port;

import java.util.Optional;
import java.util.UUID;

import com.zetcom.mpria.catalog.domain.RecordSchema;

/**
 * Outbound port for the compiled schema of a record type.
 *
 * <p>Schemas originate in the configuration service. Because a published revision is immutable,
 * the adapter behind this port may cache indefinitely — which is what lets the configuration
 * service be unavailable without stopping cataloguing.
 */
public interface RecordSchemaProvider {

    Optional<RecordSchema> schemaFor(UUID tenantId, String recordType);
}
