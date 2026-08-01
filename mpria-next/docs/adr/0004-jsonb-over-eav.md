# ADR-0004 — Spine columns plus `jsonb` instead of DOM containers / EAV

* Status: Accepted
* Date: 2026-08-01

## Context

MpRIA v8 stores configurable records in a generic "DOM container" data dictionary — effectively
entity-attribute-value. It delivers unlimited configurability and costs: no meaningful foreign
keys, no natural indexes, queries composed of joins over joins, and a schema no operator can read.
The scale of the accidental complexity is visible in `MpDAO`, which carries roughly one hundred
`DomContainerNNDaoTest` classes.

Requirement #9 asks for a simple, maintainable, scalable database design — while still supporting
customer-configured fields.

## Decision

One table per aggregate. Fixed **spine columns** for everything the platform itself needs, and a
single `jsonb` **payload column** for the customer-configured fields.

```sql
catalog_record(id, tenant_id, record_type, schema_version, identifier, title,
               lifecycle, data jsonb, org_unit_id, audit…, version)
```

Configured field definitions live in `config-service`, published per
`(record_type, schema_version)`. `catalog-service` compiles them into a **payload validator** and
checks every write against it.

## Rationale

* **Readable.** `SELECT * FROM catalog_record LIMIT 1` tells a new developer what a record is.
* **No DDL for configuration.** Adding a field is a config revision. No migration, no downtime,
  no per-customer schema drift.
* **Indexable.** A GIN index on `data jsonb_path_ops` serves ad-hoc predicates. Fields that
  become hot are promoted to `GENERATED ALWAYS AS ((data->>'x')) STORED` with a btree index — a
  measured optimisation rather than an upfront guess.
* **Real foreign keys** on the relation table, which EAV could not provide.
* **Not schema-less.** That validation gate is what separates this from a document-store
  free-for-all: the payload is exactly as constrained as the configuration says it is.
* **Versioned.** `schema_version` per row means a config change never breaks stored data; an
  upcaster normalises on read and a background job rewrites lazily.

## Rejected alternatives

* **EAV / DOM containers** — the incumbent. Rejected on maintainability, which is the explicit ask.
* **Table-per-record-type, generated DDL** — best query performance and strongest typing.
  Rejected: config publishing becomes online schema migration across N customer databases, which
  is a far worse operational problem than a GIN index.
* **Document database (MongoDB)** — natural fit for the payload, but loses relational integrity
  for relations, ACL joins, and transactions, and adds a datastore. Postgres `jsonb` gives most of
  the benefit with none of those losses.

## Consequences

* Queries on unindexed `jsonb` paths degrade on large tables. Mitigation: the read path for search
  and faceting is OpenSearch, not Postgres. Postgres serves single-record and relation queries.
* `jsonb` payloads must not grow unbounded. Guardrail: a configured record type is capped
  (default 200 fields / 1 MB serialised) and the cap is a validation rule, checked in tests.
* Reporting tools that expect columns need a view layer. Provide per-record-type SQL views
  generated from the configuration.
