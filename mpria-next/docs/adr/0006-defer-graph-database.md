# ADR-0006 — Defer a dedicated graph database; adopt pgvector now

* Status: Accepted
* Date: 2026-08-01

## Context

Requirement #9 asks for "graph or vector databases for search". Collection management is genuinely
relationship-heavy: objects relate to people, exhibitions, locations, events, and to each other.
That reads like a graph problem.

## Decision

* **Vector search: adopt now.** `pgvector` in the search context, for semantic similarity over
  title and narrative fields.
* **Full-text and facets: OpenSearch.** One index per `(tenant, record_type)`.
* **Graph: defer.** Relationships are stored as an explicit edge table in `catalog-service` and
  traversed with recursive CTEs. No dedicated graph database in the initial architecture.

## Rationale

* The traversals a collection system actually performs are shallow and typed — "this exhibition's
  objects, their owners, their current locations" is three hops with known edge types. Postgres
  recursive CTEs over an indexed edge table handle that well.
* A graph database would be a **third datastore** with its own backup, upgrade, monitoring, and
  on-prem installation story — carried for a query pattern not yet demonstrated. Requirement #7
  (self-hostable) makes every additional datastore expensive.
* `pgvector` is different: it is an extension to a database already deployed, so semantic search
  costs nearly nothing operationally.
* Deferring is cheap to reverse. The edge table *is* a graph; projecting it into Neo4j or enabling
  Apache AGE inside the same Postgres is an additive change, not a migration.

## Revisit trigger

Adopt a graph engine when **either** holds, measured not assumed:

1. A required query exceeds four hops or needs variable-length path semantics; or
2. A production traversal cannot be served under 200 ms p95 by a tuned recursive CTE.

Preferred escalation order: Apache AGE (same Postgres, no new deployment) → Neo4j (separate
projection fed by `RecordChanged` events).

## Consequences

* The edge table must be designed with traversal in mind from the start: indexes on
  `(tenant_id, from_record, relation_type)` and `(tenant_id, to_record, relation_type)`, and edges
  stored in one direction with the reverse resolved by query, not duplicated.
* Recursive CTE depth must be explicitly bounded in every query to prevent a cycle from producing
  an unbounded scan.
