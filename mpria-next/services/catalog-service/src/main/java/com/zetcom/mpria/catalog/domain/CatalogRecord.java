package com.zetcom.mpria.catalog.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The catalogue record aggregate — the one transactional boundary in this context.
 *
 * <p>Structure follows ADR-0004: fixed spine attributes for everything the platform itself needs,
 * plus a single payload map for the customer-configured fields. There is no entity-attribute-value
 * table and no per-record-type DDL; adding a field is a configuration change.
 *
 * <p>Deliberately free of Spring, JPA and HTTP. Persistence maps onto it from
 * {@code infrastructure.persistence}; the compiler cannot see that direction and ArchUnit forbids
 * the reverse.
 */
public final class CatalogRecord {

    private final UUID id;
    private final UUID tenantId;
    private final String recordType;
    private final UUID orgUnitId;
    private final Instant createdAt;
    private final String createdBy;

    private int schemaVersion;
    private String identifier;
    private String title;
    private Lifecycle lifecycle;
    private Map<String, Object> data;
    private Instant updatedAt;
    private String updatedBy;
    private long version;

    private CatalogRecord(UUID id, UUID tenantId, String recordType, UUID orgUnitId,
                          Instant createdAt, String createdBy) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.recordType = Objects.requireNonNull(recordType, "recordType");
        this.orgUnitId = Objects.requireNonNull(orgUnitId, "orgUnitId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
    }

    /**
     * Creates a record, rejecting the write if the payload does not satisfy the schema.
     *
     * @throws RecordValidationException with every field's messages at once — a cataloguer should
     *                                   see all the problems in one pass, not one per attempt
     */
    public static CatalogRecord create(UUID id,
                                       UUID tenantId,
                                       UUID orgUnitId,
                                       RecordSchema schema,
                                       String identifier,
                                       Map<String, Object> payload,
                                       String actor,
                                       Instant now) {
        requireValid(schema, payload);

        CatalogRecord record = new CatalogRecord(id, tenantId, schema.recordType(), orgUnitId, now, actor);
        record.schemaVersion = schema.schemaVersion();
        record.identifier = identifier;
        record.lifecycle = Lifecycle.DRAFT;
        record.data = schema.normalise(payload);
        record.title = schema.titleOf(record.data).orElse(identifier);
        record.updatedAt = now;
        record.updatedBy = actor;
        record.version = 0L;
        return record;
    }

    /** Rehydrates an already-persisted record. Used only by the persistence adapter. */
    public static CatalogRecord rehydrate(UUID id, UUID tenantId, String recordType, UUID orgUnitId,
                                          int schemaVersion, String identifier, String title,
                                          Lifecycle lifecycle, Map<String, Object> data,
                                          Instant createdAt, String createdBy,
                                          Instant updatedAt, String updatedBy, long version) {
        CatalogRecord record = new CatalogRecord(id, tenantId, recordType, orgUnitId, createdAt, createdBy);
        record.schemaVersion = schemaVersion;
        record.identifier = identifier;
        record.title = title;
        record.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        record.data = data == null ? Map.of() : Map.copyOf(data);
        record.updatedAt = updatedAt;
        record.updatedBy = updatedBy;
        record.version = version;
        return record;
    }

    /**
     * Replaces the payload.
     *
     * <p>Re-stamps {@code schemaVersion} to the schema being written against, so a record silently
     * migrates forward the next time anybody edits it.
     */
    public void update(RecordSchema schema, String identifier, Map<String, Object> payload,
                       String actor, Instant now) {
        if (!schema.recordType().equals(recordType)) {
            throw new IllegalArgumentException(
                    "cannot apply a '" + schema.recordType() + "' schema to a '" + recordType + "' record");
        }
        if (!lifecycle.allowsEditing()) {
            throw new IllegalStateException("record " + id + " is " + lifecycle + " and cannot be edited");
        }
        requireValid(schema, payload);

        this.schemaVersion = schema.schemaVersion();
        this.identifier = identifier;
        this.data = schema.normalise(payload);
        this.title = schema.titleOf(this.data).orElse(identifier);
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    public void transitionTo(Lifecycle target, String actor, Instant now) {
        if (!lifecycle.canTransitionTo(target)) {
            throw new IllegalStateException("cannot move record " + id + " from " + lifecycle + " to " + target);
        }
        this.lifecycle = target;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    private static void requireValid(RecordSchema schema, Map<String, Object> payload) {
        Map<String, List<String>> errors = schema.validate(payload);
        if (!errors.isEmpty()) {
            throw new RecordValidationException(schema.recordType(), errors);
        }
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public String recordType() {
        return recordType;
    }

    public UUID orgUnitId() {
        return orgUnitId;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public String identifier() {
        return identifier;
    }

    public String title() {
        return title;
    }

    public Lifecycle lifecycle() {
        return lifecycle;
    }

    public Map<String, Object> data() {
        return Map.copyOf(data);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String createdBy() {
        return createdBy;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public String updatedBy() {
        return updatedBy;
    }

    public long version() {
        return version;
    }

    /** Optimistic-concurrency token handed to clients and echoed back on write. */
    public String etag() {
        return "\"" + version + "\"";
    }
}
