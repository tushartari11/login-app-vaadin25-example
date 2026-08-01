# ADR-0007 — Microservices, each internally a Spring Modulith

* Status: Accepted
* Date: 2026-08-01

## Context

The brief asks for "a scalable microservices architecture from the start". The counter-argument is
well known: a small team building a greenfield product usually cannot yet see where the seams are,
and premature decomposition converts every design mistake into a distributed-systems problem.

The decision was raised with the requester and reaffirmed. This ADR records the decision and the
hedge that makes it survivable.

## Decision

Decompose into independently deployable services along **data-ownership** lines
(`config`, `catalog`, `search`, `media`, `access`, `presentation-bff`). Additionally, structure
**every** service internally with Spring Modulith: explicit modules, verified dependency
directions, and in-process events between modules.

## Rationale for the hedge

* Modulith boundaries are *cheap* to move and service boundaries are *expensive* to move. Keeping
  both means a wrong cut can be corrected inside a service before it hardens into a network call.
* `spring-modulith-events-jpa` gives a transactional outbox for free, which is exactly what §5.2's
  saga model needs — the same code publishes an in-process event today and a Kafka event tomorrow.
* `ApplicationModules.verify()` runs as a unit test. Boundary erosion fails the build instead of
  accumulating.
* If two services turn out to share a transaction boundary, merging them is: move a module, delete
  a client, delete a deployment. The domain code is untouched.

## Boundaries chosen, and why they are safe

| Service | Owns | Why it is genuinely separate |
| --- | --- | --- |
| `config-service` | Configuration revisions | Admin-time write rate; different users; must run when nothing else does |
| `catalog-service` | Records and relations | The transactional write core |
| `search-service` | Indices, embeddings | Read-only projection; scales on a different axis; may be down without data loss |
| `media-service` | Binaries, derivatives | Different resource profile (CPU/IO for derivatives); different storage |
| `access-service` | Org units, grants | Referenced by everything, written rarely; a natural policy boundary |
| `presentation-bff` | Nothing | Stateless; scales with UI traffic, independent of write load |

Note what is *not* split: relations do not get their own service, because a relation and the
records it joins share a transaction boundary. Splitting them would have manufactured a
distributed transaction for no benefit — the exact failure this ADR exists to avoid.

## Consequences

* Six deployment pipelines, six sets of dashboards, six on-prem containers. The Compose bundle and
  Helm chart must hide that from the customer.
* Cross-service debugging requires distributed tracing to be working on day one, not later.
  Non-negotiable; see ARCHITECTURE.md §11.
* Contract tests (Pact) become a hard CI gate. Without them, independent deployability is a claim
  rather than a property.
