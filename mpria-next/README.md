# MpRIA Next

A framework-agnostic, service-oriented successor to MpRIA (Vaadin 8 / Payara 5 / Java 8).

Keeps what is worth keeping — the `View → PresentationModel → ApplicationModel → Repository`
separation, and configuration-over-code. Drops what is not — Vaadin, Groovy custom code, DOM
containers, and server-side UI session state.

> **Status: Phase 0, walking skeleton.** The vertical slice is real and tested. Most of the
> product is not built. See [`ARCHITECTURE.md` §15](ARCHITECTURE.md#15-roadmap) for what comes next.

---

## Documents

| Document | What it is for |
| --- | --- |
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | Start here. Bounded contexts, transaction boundaries, data model, auth, observability, testing, deployment. |
| [docs/CONTRACTS.md](docs/CONTRACTS.md) | The frozen inter-service HTTP contracts. Binding. |
| [docs/ui-configuration.md](docs/ui-configuration.md) | The admin application and the drag-and-drop layout designer, in depth. |
| [docs/adr/](docs/adr/) | Seven decision records, each with its rejected alternatives and its revisit trigger. |

---

## The idea in one diagram

```
  Admin console ──────▶ config-service          (layouts, field definitions, revisions)
                              │
                              │ layout + schema
                              ▼
  Web client ◀── PresentationModel JSON ── presentation-bff
  (generic                                       │
   renderer)                                     │ data
                                                 ▼
                                         catalog-service ──▶ Postgres (spine + jsonb)
```

The web client asks for **a screen**, not for **an object**, and renders whatever tree it is
handed. It contains no domain knowledge at all — no field names, no record types. That is what
makes the frontend framework replaceable, and it is enforced by a test that fails if a domain
field name appears anywhere in the renderer's test suite.

---

## Repository layout

```
mpria-next/
├── ARCHITECTURE.md
├── docs/                       CONTRACTS.md, ui-configuration.md, adr/
├── pom.xml                     Maven aggregator, Java 21, Spring Boot 4.1
├── services/
│   ├── pm-contract/            the PresentationModel wire types. No Spring, no UI, no persistence
│   ├── catalog-service/        the transactional write core  (8081)
│   ├── config-service/         the administration application (8082)
│   └── presentation-bff/       the presentation-model layer   (8080)
├── web/                        Next.js generic renderer       (3000)
└── platform/
    ├── compose/                local stack
    └── keycloak/               development realm
```

---

## Running it

### Prerequisites

Java 21 (`zulu-21` is on this machine), Maven 3.9+, Node 24+, Docker.

### 1. Infrastructure

```bash
cd platform/compose
docker compose up -d              # Postgres + Keycloak
```

Keycloak lands on <http://localhost:8180> (`admin` / `admin`), importing the `mpria` realm with
three users: `curator`, `configadmin`, `reader` (password = username).

Optional profiles:

```bash
docker compose --profile observability up -d   # OTel collector, Prometheus, Grafana, OpenSearch
docker compose --profile apps up -d --build    # the services as containers (needs mvn package first)
```

### 2. Build and test

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
mvn -B -s .mvn/settings-central.xml clean verify
```

> **On the corporate network**, drop `-s .mvn/settings-central.xml` so artifacts resolve through
> nexus.zetcom.group as usual. That flag exists only because the Nexus mirror is unreachable
> off-VPN; see the comment in [`.mvn/settings-central.xml`](.mvn/settings-central.xml).

`verify` runs unit tests, the ArchUnit suite, and the Testcontainers integration tests (Docker
must be running).

### 3. Services

```bash
mvn -B -pl services/config-service      spring-boot:run    # 8082
mvn -B -pl services/catalog-service     spring-boot:run    # 8081
mvn -B -pl services/presentation-bff    spring-boot:run    # 8080
cd web && npm install && npm run dev                       # 3000
```

### 4. A token, for poking at it by hand

```bash
curl -s -X POST http://localhost:8180/realms/mpria/protocol/openid-connect/token \
  -d grant_type=password -d client_id=mpria-dev-cli \
  -d username=curator -d password=curator | jq -r .access_token
```

The `mpria-dev-cli` client has direct access grants enabled and exists **only** for this. The real
clients use Authorization Code + PKCE.

The web client needs that token too — it holds it server-side and never in `localStorage`, so pass
it in the environment rather than the browser:

```bash
cd web && MPRIA_DEV_ACCESS_TOKEN="$(…the curl above…)" npm run dev
```

Without it, `/screens/<screenId>/<recordId>` calls the BFF unauthenticated. `/demo` needs no token
and no backend at all. See [`web/README.md`](web/README.md) for the Phase 1 PKCE target.

---

## What is deliberately not built yet

Honesty about skeleton scope, so nobody mistakes a demo for a product:

| Not built | Where it is designed | Phase |
| --- | --- | --- |
| Relations between records (edge table, recursive CTE traversal) | ARCHITECTURE.md §9.3 | 1 |
| The drag-and-drop layout designer UI | docs/ui-configuration.md §4 | 2 |
| Configuration persistence — `config-service` seeds from a classpath JSON, in memory | docs/ui-configuration.md §2 | 2 |
| Signed bundle zips (manifest, checksum, signature) and base + overlay merge — plain JSON export/import with a dry-run diff is built | docs/ui-configuration.md §5 | 2 |
| Search: OpenSearch projection, pgvector | ADR-0006 | 3 |
| `access-service`, org units, fine-grained ACL | ARCHITECTURE.md §10 | 4 |
| `media-service` | ARCHITECTURE.md §5 | 5 |
| Kafka, transactional outbox, the event-driven cache invalidation the schema provider is stubbed for | ARCHITECTURE.md §5.2 | 1 |
| Client-credentials service-to-service auth (the skeleton relays the caller's token) | ARCHITECTURE.md §10 | 1 |
| Helm chart, jib images, GitOps | ARCHITECTURE.md §13 | 6 |

---

## Conventions worth knowing before contributing

- **Tests come first.** No production class exists before a failing test names it.
- **The domain imports nothing.** No Spring, no JPA, no Jackson, no HTTP under `domain/`.
  `ArchitectureTest` fails the build otherwise — this is the boundary MpRIA v8 lost gradually.
- **Hibernate, not EclipseLink.** The jsonb payload maps via `@JdbcTypeCode(SqlTypes.JSON)`. A
  Maven Enforcer rule bans `org.eclipse.persistence:*` so it cannot arrive transitively.
- **One writer per aggregate.** No shared databases, no cross-service writes in one request.
- **The BFF may read from many services; it may write to at most one.** Multi-service writes are
  sagas owned by a domain service.
- **No domain vocabulary in framework code.** The framework knows `RecordType`, `Field`,
  `Relation`. It must never know `Artist` or `Painting`. Customer field names appear only in test
  fixtures and configuration.

### Spring Boot 4 notes, learned the hard way

- Jackson 3: `tools.jackson.databind.*`. Annotations stay `com.fasterxml.jackson.annotation.*`.
- Test slices moved out of `spring-boot-test-autoconfigure` into per-module starters —
  `@DataJpaTest` needs `spring-boot-starter-data-jpa-test`, `@WebMvcTest` needs
  `spring-boot-starter-webmvc-test`. `spring-boot-starter-test` alone no longer provides them.
- `@MockBean` is gone; use `@MockitoBean`.
- `HttpStatus.UNPROCESSABLE_ENTITY` is deprecated in favour of `UNPROCESSABLE_CONTENT`.
- Testcontainers 2 renamed the modules to `testcontainers-<module>`, and
  `PostgreSQLContainer` is no longer generic.
