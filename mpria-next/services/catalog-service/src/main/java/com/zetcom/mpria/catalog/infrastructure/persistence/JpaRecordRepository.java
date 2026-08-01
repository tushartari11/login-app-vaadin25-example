package com.zetcom.mpria.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.zetcom.mpria.catalog.application.RecordConcurrentlyModifiedException;
import com.zetcom.mpria.catalog.application.port.RecordRepository;
import com.zetcom.mpria.catalog.domain.CatalogRecord;

import org.springframework.data.domain.Limit;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

/** Adapter binding the {@link RecordRepository} port to Hibernate. */
@Repository
class JpaRecordRepository implements RecordRepository {

    private final CatalogRecordJpaRepository jpa;

    JpaRecordRepository(CatalogRecordJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<CatalogRecord> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(CatalogRecordEntity::toDomain);
    }

    @Override
    public List<CatalogRecord> findByType(UUID tenantId, String recordType, int limit) {
        return jpa.findByTenantIdAndRecordTypeOrderByUpdatedAtDesc(tenantId, recordType, Limit.of(limit))
                .stream()
                .map(CatalogRecordEntity::toDomain)
                .toList();
    }

    @Override
    public CatalogRecord save(CatalogRecord record) {
        CatalogRecordEntity entity = jpa.findByIdAndTenantId(record.id(), record.tenantId())
                .map(existing -> merge(existing, record))
                .orElseGet(() -> CatalogRecordEntity.fromDomain(record));

        try {
            return jpa.saveAndFlush(entity).toDomain();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RecordConcurrentlyModifiedException(record.id(), e);
        }
    }

    /**
     * Copies the aggregate's state onto the stored row, but only if the aggregate was loaded from
     * the version still on disk.
     *
     * <p>The explicit comparison is load-bearing. Hibernate's {@code @Version} check compares the
     * <em>managed</em> entity against the database, and inside one persistence context both a
     * winning and a losing writer hold the same managed instance — so the winner's commit silently
     * advances the version the loser is then measured against, and the stale write succeeds.
     * Checking the domain object's own version closes that window and makes this adapter behave
     * exactly like the in-memory fake the application tests run against.
     */
    private static CatalogRecordEntity merge(CatalogRecordEntity stored, CatalogRecord record) {
        if (stored.getVersion() != record.version()) {
            throw new RecordConcurrentlyModifiedException(record.id(), null);
        }
        stored.applyMutableState(record);
        return stored;
    }
}
