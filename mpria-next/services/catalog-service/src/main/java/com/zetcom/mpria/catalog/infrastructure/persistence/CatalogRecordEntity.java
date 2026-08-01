package com.zetcom.mpria.catalog.infrastructure.persistence;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.zetcom.mpria.catalog.domain.CatalogRecord;
import com.zetcom.mpria.catalog.domain.Lifecycle;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Hibernate mapping for the record spine plus its {@code jsonb} payload.
 *
 * <p>Kept strictly separate from {@link CatalogRecord}: the domain aggregate carries the
 * invariants and knows nothing about persistence, and this entity carries the mapping and knows
 * nothing about the rules. The conversion lives in {@link #toDomain()} and
 * {@link #fromDomain(CatalogRecord)}.
 *
 * <p>The payload maps through Hibernate's {@code @JdbcTypeCode(SqlTypes.JSON)}, which binds a
 * {@code Map} straight onto a Postgres {@code jsonb} column — no converter, no string round-trip,
 * and the column stays queryable and GIN-indexable from SQL.
 */
@Entity
@Table(name = "catalog_record")
class CatalogRecordEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "record_type", nullable = false, updatable = false)
    private String recordType;

    @Column(name = "org_unit_id", nullable = false)
    private UUID orgUnitId;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle", nullable = false)
    private Lifecycle lifecycle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false)
    private Map<String, Object> data = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CatalogRecordEntity() {
        // required by Hibernate
    }

    static CatalogRecordEntity fromDomain(CatalogRecord record) {
        CatalogRecordEntity entity = new CatalogRecordEntity();
        entity.id = record.id();
        entity.tenantId = record.tenantId();
        entity.recordType = record.recordType();
        entity.orgUnitId = record.orgUnitId();
        entity.createdAt = record.createdAt();
        entity.createdBy = record.createdBy();
        entity.version = record.version();
        entity.applyMutableState(record);
        return entity;
    }

    void applyMutableState(CatalogRecord record) {
        this.schemaVersion = record.schemaVersion();
        this.identifier = record.identifier();
        this.title = record.title();
        this.lifecycle = record.lifecycle();
        this.data = new LinkedHashMap<>(record.data());
        this.updatedAt = record.updatedAt();
        this.updatedBy = record.updatedBy();
    }

    CatalogRecord toDomain() {
        return CatalogRecord.rehydrate(id, tenantId, recordType, orgUnitId, schemaVersion,
                identifier, title, lifecycle, data, createdAt, createdBy, updatedAt, updatedBy, version);
    }

    UUID getId() {
        return id;
    }

    long getVersion() {
        return version;
    }
}
