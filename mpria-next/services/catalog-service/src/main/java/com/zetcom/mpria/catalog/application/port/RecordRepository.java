package com.zetcom.mpria.catalog.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.zetcom.mpria.catalog.domain.CatalogRecord;

/**
 * Outbound port for record storage.
 *
 * <p>Every method is tenant-scoped by parameter rather than by ambient state. A repository that
 * could be called without a tenant is a repository that will eventually be called without one.
 */
public interface RecordRepository {

    Optional<CatalogRecord> findById(UUID tenantId, UUID id);

    List<CatalogRecord> findByType(UUID tenantId, String recordType, int limit);

    /**
     * Inserts or updates, honouring optimistic locking.
     *
     * @throws com.zetcom.mpria.catalog.application.RecordConcurrentlyModifiedException
     *         when the stored version has moved on since this copy was loaded
     */
    CatalogRecord save(CatalogRecord record);
}
