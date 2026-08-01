package com.zetcom.mpria.catalog.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zetcom.mpria.catalog.application.port.RecordRepository;
import com.zetcom.mpria.catalog.application.port.RecordSchemaProvider;
import com.zetcom.mpria.catalog.domain.CatalogRecord;
import com.zetcom.mpria.catalog.domain.Lifecycle;
import com.zetcom.mpria.catalog.domain.RecordNotFoundException;
import com.zetcom.mpria.catalog.domain.RecordSchema;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for cataloguing.
 *
 * <p>This class owns the transaction boundary, and the boundary is exactly one aggregate: a record
 * and its own fields commit together, and nothing else joins that transaction. Effects that reach
 * other contexts (indexing, derivative generation) travel as events after commit, never as a
 * second write inside it — see ARCHITECTURE.md §5.2.
 *
 * <p>Business rules live in the domain. What lives here is orchestration: load, delegate, save.
 */
@Service
public class CatalogRecordService {

    private final RecordRepository records;
    private final RecordSchemaProvider schemas;
    private final Clock clock;

    public CatalogRecordService(RecordRepository records, RecordSchemaProvider schemas, Clock clock) {
        this.records = records;
        this.schemas = schemas;
        this.clock = clock;
    }

    @Transactional
    public CatalogRecord create(CatalogPrincipal principal,
                                String recordType,
                                String identifier,
                                Map<String, Object> payload) {
        RecordSchema schema = requireSchema(principal.tenantId(), recordType);
        CatalogRecord record = CatalogRecord.create(
                UUID.randomUUID(),
                principal.tenantId(),
                principal.orgUnitId(),
                schema,
                identifier,
                payload,
                principal.userId(),
                clock.instant());
        return records.save(record);
    }

    @Transactional
    public CatalogRecord update(CatalogPrincipal principal,
                                UUID recordId,
                                String identifier,
                                Map<String, Object> payload,
                                String expectedEtag) {
        CatalogRecord record = require(principal.tenantId(), recordId);
        requireCurrent(record, expectedEtag);

        RecordSchema schema = requireSchema(principal.tenantId(), record.recordType());
        record.update(schema, identifier, payload, principal.userId(), clock.instant());
        return records.save(record);
    }

    @Transactional
    public CatalogRecord transition(CatalogPrincipal principal, UUID recordId, Lifecycle target) {
        CatalogRecord record = require(principal.tenantId(), recordId);
        record.transitionTo(target, principal.userId(), clock.instant());
        return records.save(record);
    }

    @Transactional(readOnly = true)
    public CatalogRecord get(CatalogPrincipal principal, UUID recordId) {
        return require(principal.tenantId(), recordId);
    }

    @Transactional(readOnly = true)
    public List<CatalogRecord> listByType(CatalogPrincipal principal, String recordType, int limit) {
        return records.findByType(principal.tenantId(), recordType, Math.clamp(limit, 1, 200));
    }

    private CatalogRecord require(UUID tenantId, UUID recordId) {
        return records.findById(tenantId, recordId)
                .orElseThrow(() -> new RecordNotFoundException(recordId));
    }

    private RecordSchema requireSchema(UUID tenantId, String recordType) {
        return schemas.schemaFor(tenantId, recordType)
                .orElseThrow(() -> new UnknownRecordTypeException(recordType));
    }

    /**
     * Rejects a write built against a stale copy. A null expectation means the caller opted out,
     * which is allowed for server-to-server callers but never sent by the BFF.
     */
    private static void requireCurrent(CatalogRecord record, String expectedEtag) {
        if (expectedEtag != null && !expectedEtag.equals(record.etag())) {
            throw new StaleRecordException(record.id(), expectedEtag, record.etag());
        }
    }
}
