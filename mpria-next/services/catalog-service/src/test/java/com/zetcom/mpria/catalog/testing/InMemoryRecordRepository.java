package com.zetcom.mpria.catalog.testing;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.zetcom.mpria.catalog.application.RecordConcurrentlyModifiedException;
import com.zetcom.mpria.catalog.application.port.RecordRepository;
import com.zetcom.mpria.catalog.domain.CatalogRecord;

/**
 * A fake, not a mock.
 *
 * <p>It actually enforces tenant scoping and optimistic locking, so an application-layer test that
 * passes against it is testing behaviour rather than its own stubbing. A Mockito stub returning
 * whatever the test asked for would let a tenant-leak bug through unnoticed.
 */
public final class InMemoryRecordRepository implements RecordRepository {

    private final Map<UUID, CatalogRecord> stored = new LinkedHashMap<>();

    @Override
    public Optional<CatalogRecord> findById(UUID tenantId, UUID id) {
        return Optional.ofNullable(stored.get(id))
                .filter(record -> record.tenantId().equals(tenantId))
                .map(InMemoryRecordRepository::copyOf);
    }

    @Override
    public List<CatalogRecord> findByType(UUID tenantId, String recordType, int limit) {
        return stored.values().stream()
                .filter(record -> record.tenantId().equals(tenantId))
                .filter(record -> record.recordType().equals(recordType))
                .sorted(Comparator.comparing(CatalogRecord::updatedAt).reversed())
                .limit(limit)
                .map(InMemoryRecordRepository::copyOf)
                .toList();
    }

    @Override
    public CatalogRecord save(CatalogRecord record) {
        CatalogRecord existing = stored.get(record.id());
        if (existing != null && existing.version() != record.version()) {
            throw new RecordConcurrentlyModifiedException(record.id(), null);
        }
        long nextVersion = existing == null ? 0L : existing.version() + 1;
        CatalogRecord persisted = copyOf(record, nextVersion);
        stored.put(record.id(), persisted);
        return copyOf(persisted);
    }

    /** Simulates a competing writer having already committed. */
    public void bumpVersion(UUID recordId) {
        CatalogRecord record = stored.get(recordId);
        stored.put(recordId, copyOf(record, record.version() + 1));
    }

    public int size() {
        return stored.size();
    }

    private static CatalogRecord copyOf(CatalogRecord record) {
        return copyOf(record, record.version());
    }

    /** Rehydrating on every read and write means callers never share a mutable aggregate. */
    private static CatalogRecord copyOf(CatalogRecord record, long version) {
        return CatalogRecord.rehydrate(
                record.id(), record.tenantId(), record.recordType(), record.orgUnitId(),
                record.schemaVersion(), record.identifier(), record.title(), record.lifecycle(),
                record.data(), record.createdAt(), record.createdBy(),
                record.updatedAt(), record.updatedBy(), version);
    }
}
