-- The catalogue write model: a fixed spine plus one jsonb payload column.
--
-- See ADR-0004. There is deliberately no entity-attribute-value structure and no per-record-type
-- DDL: adding a configured field is a configuration revision, not a migration.

CREATE TABLE catalog_record
(
    id             uuid         PRIMARY KEY,
    tenant_id      uuid         NOT NULL,
    record_type    text         NOT NULL,
    org_unit_id    uuid         NOT NULL,
    schema_version integer      NOT NULL,
    identifier     text,
    title          text,
    lifecycle      text         NOT NULL,
    data           jsonb        NOT NULL DEFAULT '{}'::jsonb,
    created_at     timestamptz  NOT NULL,
    created_by     text         NOT NULL,
    updated_at     timestamptz  NOT NULL,
    updated_by     text         NOT NULL,
    version        bigint       NOT NULL DEFAULT 0,

    CONSTRAINT catalog_record_lifecycle_known
        CHECK (lifecycle IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    -- Guardrail from ADR-0004: the payload is configurable, not unbounded.
    CONSTRAINT catalog_record_payload_bounded
        CHECK (pg_column_size(data) <= 1048576)
);

-- Serves ad-hoc predicates over configured fields without an index per field.
CREATE INDEX catalog_record_data_gin
    ON catalog_record USING gin (data jsonb_path_ops);

-- The common list query: this tenant's records of one type, most recently touched first.
CREATE INDEX catalog_record_tenant_type_updated
    ON catalog_record (tenant_id, record_type, updated_at DESC);

-- Inventory numbers are unique per tenant and record type where they are set at all.
CREATE UNIQUE INDEX catalog_record_identifier_unique
    ON catalog_record (tenant_id, record_type, identifier)
    WHERE identifier IS NOT NULL;

COMMENT ON COLUMN catalog_record.data IS
    'Customer-configured fields. Validated on write against the JSON schema compiled from the '
    'published configuration revision; never written unvalidated.';

COMMENT ON COLUMN catalog_record.schema_version IS
    'Configuration schema version this payload was written under. Old payloads stay readable; an '
    'upcaster normalises on read and a background job rewrites lazily.';
