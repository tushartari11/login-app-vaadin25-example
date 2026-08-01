# MpRIA Next — project memory / session handover

> **Read this first if you are a new session, or a session that lost its context.**
> It is the single document that explains what this project is, what state it is in, how to build
> and run it, and every non-obvious thing that cost time to discover.
>
> Last updated: 2026-08-01 · Phase 0 (walking skeleton) complete and green.

---

## 1. What this is, and what it is not

A **greenfield successor to MpRIA**, the Vaadin 8 / Payara 5 / Java 8 collection-management
framework that lives in the sibling directory `~/development/mpria/framework-vaadin8`.

**This is a separate repository, not a branch.** Nothing here is built from, or deploys alongside,
the v8 monolith. Do not import v8 code, and do not assume v8 constraints (Java 8, Maven 3.9.11
pinning, the applet-plugin `JAVA_HOME` trap) apply here — they do not.

MpRIA's product value is that customers do not get a museum application, they get a **configurable
one**. That property is the one thing the rewrite must not lose. Everything else was renegotiated.

### Decisions the user made explicitly (do not relitigate these)

| Question | Decision |
| --- | --- |
| Migrate v8 customer data? | **No — clean slate.** A DOM-container → jsonb ETL is a later, separate project |
| Where does the presentation model live? | **Server-side Java, emitted as JSON** to a generic renderer. *Not* a TypeScript PM in the client |
| Deployment target | **Self-hosted on-prem for the MVP**, moving to SaaS as the customer base grows *while keeping on-prem possible* |
| Architecture | **Microservices from the start** — the user was told the cost and reaffirmed it |
| Persistence provider | **Hibernate ORM, explicitly not EclipseLink** (user's call; enforced by a Maven Enforcer ban) |
| Deliverable | Tech doc **and** a runnable walking skeleton |

The on-prem-first answer is load-bearing: it is why every infrastructure dependency is
self-hostable OSS (Keycloak, Postgres, Kafka/Redpanda, OpenSearch, Grafana) and why no managed
cloud service is allowed to become structural.

---

## 2. Where everything is

```
~/development/mpria/mpria-next/
├── mpria-next-own-memory.md    ← you are here
├── README.md                   how to run it; skeleton-scope honesty table
├── ARCHITECTURE.md             THE design doc — contexts, layering, data, auth, testing, roadmap
├── docs/
│   ├── CONTRACTS.md            BINDING inter-service wire shapes. Read before touching any API
│   ├── ui-configuration.md     the admin app + drag-and-drop layout designer, in depth
│   └── adr/0001..0007          decisions, each with rejected alternatives and a revisit trigger
├── .agents/                    the multi-agent build team: briefs, launcher, completion reports
├── .mvn/settings-central.xml   off-VPN Maven settings (see §4)
├── .claude/settings.json       shared permission allow-list for agents
├── pom.xml                     aggregator: Java 21, Spring Boot 4.1.0
├── services/
│   ├── pm-contract/            PresentationModel wire types. No Spring, no UI, no persistence
│   ├── catalog-service/        transactional write core        :8081
│   ├── config-service/         the administration application  :8082
│   └── presentation-bff/       the presentation-model layer    :8080
├── web/                        Next.js 15 generic renderer     :3000
└── platform/
    ├── compose/                Postgres + Keycloak, plus `apps` and `observability` profiles
    └── keycloak/               development realm + its own README of import traps
```

Reading order for a newcomer: this file → `ARCHITECTURE.md` §1–§7 → `docs/CONTRACTS.md`.

---

## 3. The architecture in one page

```
  Admin console ──────▶ config-service        layouts, field definitions, immutable revisions
                              │
                              │ layout + schema (pinned to a revision, cached forever)
                              ▼
  Web client ◀── PresentationModel JSON ── presentation-bff
  (generic renderer,                            │
   zero domain knowledge)                       │ record data
                                                ▼
                                        catalog-service ──▶ Postgres (spine columns + jsonb)
```

**The load-bearing idea:** the web client asks for *a screen*, not for *an object*. It receives a
JSON tree — `meta`, `layout`, `fields`, `actions` — and renders it through a component registry
keyed on `component.type`. It contains no field names and no record types. Swapping Next.js for
anything else means rewriting a renderer, not an application. That is what makes requirement
"no dependency on a frontend framework" real rather than aspirational.

**The v8 layering survives**, mapped onto services:

| v8 | MpRIA Next |
| --- | --- |
| `MpApplicationVaadinView` | `web/` — a generic renderer |
| `MpApplicationModel` (PresentationModel) | `presentation-bff` — Java, emits JSON, **not** in the browser |
| `MpServices` (EJB) | each service's `application/` — use cases, `@Transactional` |
| domain (scattered in v8) | each service's `domain/` — pure, imports nothing |
| `MpDAO` / `MpJPA` | each service's `infrastructure/persistence/` |

Inside every service: `domain/` ← `application/` ← `infrastructure/` + `api/`, with the direction
enforced by an ArchUnit `ArchitectureTest`. **This is the boundary v8 lost gradually; the test is
what stops that recurring.**

### Data model (ADR-0004) — the replacement for DOM containers

One table per aggregate: fixed **spine columns** the platform needs, plus one `jsonb` **payload**
for customer-configured fields.

```sql
catalog_record(id, tenant_id, record_type, schema_version, identifier, title,
               lifecycle, data jsonb, org_unit_id, created_*, updated_*, version)
```

Not schema-less: `config-service` publishes field definitions, `catalog-service` compiles them into
a payload validator and rejects every non-conforming write. Adding a field is a config revision —
no DDL, no migration. GIN index on `data jsonb_path_ops`; hot fields get promoted to generated
columns later. `tenant_id` is on every table **from day one** even though the MVP is single-tenant.

---

## 4. Building and running — exact commands

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
cd ~/development/mpria/mpria-next
mvn -B -s .mvn/settings-central.xml clean verify
```

**Both flags matter.**
`-s .mvn/settings-central.xml` is required **off-VPN**: `~/.m2/settings.xml` mirrors everything
through `nexus.zetcom.group`, which is unreachable without the VPN and fails with a connect
timeout on the Spring Boot parent POM. **On VPN, drop the flag** so artifacts stay proxied.
`-B` keeps Maven in batch mode.

Infrastructure:

```bash
cd platform/compose && docker compose up -d          # Postgres + Keycloak
docker compose --profile observability up -d         # OTel collector, Prometheus, Grafana, OpenSearch
docker compose --profile apps up -d --build          # services as containers (needs mvn package)
```

Services and client:

```bash
mvn -B -pl services/config-service   spring-boot:run   # 8082
mvn -B -pl services/catalog-service  spring-boot:run   # 8081
mvn -B -pl services/presentation-bff spring-boot:run   # 8080
cd web && npm install && npm run dev                   # 3000   ( /demo works with no backend )
cd web && npm run test && npm run build
```

A token for poking at it by hand:

```bash
curl -s -X POST http://localhost:8180/realms/mpria/protocol/openid-connect/token \
  -d grant_type=password -d client_id=mpria-dev-cli \
  -d username=curator -d password=curator | jq -r .access_token
```

Realm users (password = username): `curator` (`mpria-user`), `configadmin`
(`+ mpria-config-admin`, `mpria-config-publisher`), `reader`. All share
`tenant_id 11111111-…` and `org_unit_id 33333333-…`, matching `CatalogFixtures` so tokens and test
data line up. `mpria-dev-cli` is the **only** client with direct access grants — never replicate it
in a real deployment.

---

## 5. Current state — verified, not assumed

Full reactor `clean verify` passes. Every number below was observed on a clean build.

| Module | Tests | Notes |
| --- | --- | --- |
| `pm-contract` | 25 | includes a Jackson 3 JSON round-trip pinning the wire shape |
| `catalog-service` | 48 unit + 8 IT | ITs use real Postgres via Testcontainers |
| `config-service` | 118 | includes a `@SpringBootTest` driving the real seed through real wiring |
| `presentation-bff` | 72 | assembler unit-tested with fakes; 422 path covered at two levels |
| `web` | 101 | plus `npm run build` clean; `/demo` prerenders static |
| **Total** | **372** | |

What the skeleton demonstrably does end to end: OIDC login → configured layout fetched from
`config-service` → BFF composes a PresentationModel → generic renderer draws it responsively →
edit → save → validation errors land on the right controls → reload.

---

## 6. Bugs found while building — keep these fixed

Four real defects surfaced, each by something other than the code's own author. Worth knowing
because each represents a class of mistake this stack invites.

1. **`RestClient.Builder` missing → catalog-service could not start at all.** Spring Boot 4 moved
   that autoconfiguration out of `spring-boot-starter-web` into `spring-boot-starter-restclient`.
   48 unit tests and 7 ITs were green while the application was unbootable, because slices never
   load the full context. Fixed by adding the starter **and** by adding
   `CatalogServiceApplicationIT` — every service must have a context-loads test for exactly this.
2. **`hidden: true` leaked the value to the browser.** The renderer correctly drops the grid cell,
   but the value still travels and stays readable in the server-rendered payload. `hidden` is a
   rendering hint, **never** a confidentiality mechanism: when hiding for authorisation reasons the
   BFF must send `value: null`. Written into `docs/CONTRACTS.md`.
3. **Client/server field-name mismatch (`values` vs `data`).** The write body was unspecified in
   the contract; the client posted `values`, the BFF read `data`. Every save would have written an
   empty record with no error anywhere. Caught only because both sides were built against one
   frozen document.
4. **Optimistic locking was silently bypassed.** `JpaRecordRepository.save()` compared the
   *managed* entity's version against itself, so inside one persistence context a stale write won.
   Fixed with an explicit `merge()` guard comparing the domain object's own version.

---

## 7. Environment traps — do not rediscover these

**Spring Boot 4.1 / Spring Framework 7 / Spring Security 7 / Jackson 3 / Hibernate 7.4.1**

- Test slices split into **per-module starters**; `spring-boot-starter-test` provides none of them:
  `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure` +
  `spring-boot-starter-data-jpa-test`; `@WebMvcTest` →
  `org.springframework.boot.webmvc.test.autoconfigure` + `spring-boot-starter-webmvc-test`;
  `@AutoConfigureTestDatabase` → `org.springframework.boot.jdbc.test.autoconfigure`;
  `jwt()` → `spring-boot-starter-security-test`.
- `RestClient.Builder` needs `spring-boot-starter-restclient` (see §6.1).
- A `@Bean` on the `@SpringBootApplication` class **is still instantiated by every `@WebMvcTest`**,
  even though the slice filters out the `@Component`s it depends on. Move adapter wiring into a
  separate `@Configuration`.
- **`jwt()` does not run your `JwtAuthenticationConverter`** — role gating you believe you are
  testing is not exercised. Pass `.authorities(...)` explicitly; unit-test the converter separately.
- `@MockBean` is gone → `@MockitoBean`.
- `HttpStatus.UNPROCESSABLE_ENTITY` deprecated → `UNPROCESSABLE_CONTENT`
  (assertion: `status().isUnprocessableContent()`).
- Jackson 3 is `tools.jackson.databind.*`; **annotations stay** `com.fasterxml.jackson.annotation.*`
  (Jackson 3 ships no annotations module of its own). `Instant` parses with no module registration.
- Testcontainers 2: artifacts renamed `testcontainers-<module>`; `PostgreSQLContainer` is **no
  longer generic** (`new PostgreSQLContainer("postgres:17-alpine")`, no diamond).
- `Map.copyOf` does **not** preserve order — use `Collections.unmodifiableMap(new LinkedHashMap<>())`
  wherever serialised order matters, or every config export becomes a noisy diff.
- `-Xlint:all` flags `this-escape` when a constructor calls an overridable method; making the class
  `final` is the honest fix.

**Keycloak realm import** (`platform/keycloak/README.md` has more)

- An **unknown top-level key** (e.g. a `"_comment"`) aborts the whole import; the container exits 1.
- Declaring a **`clientScopes` array replaces the realm's built-in scopes** rather than adding to
  them, so `roles` / `profile` / `email` / `basic` are never created. The symptom is nasty: tokens
  issue fine and carry your custom claims, but `realm_access` is `null` and `preferred_username` is
  absent — so **every role check silently fails while authentication appears to work**. Use
  per-client `protocolMappers` instead. That is why the realm file looks repetitive.

---

## 8. What is deliberately NOT built

So nobody mistakes a walking skeleton for a product. All of it is designed; none of it is coded.

| Not built | Designed in | Phase |
| --- | --- | --- |
| Relations between records (edge table, recursive CTE traversal) | ARCHITECTURE.md §9.3 | 1 |
| Kafka, transactional outbox, event-driven config-cache invalidation | ARCHITECTURE.md §5.2 | 1 |
| Client-credentials service-to-service auth — **the skeleton relays the caller's token** | ARCHITECTURE.md §10 | 1 |
| The drag-and-drop layout designer UI | ui-configuration.md §4 | 2 |
| Configuration persistence — `config-service` seeds from classpath JSON into memory | ui-configuration.md §2 | 2 |
| Signed bundles + base/overlay merge (plain JSON export/import with dry-run diff *is* built) | ui-configuration.md §5 | 2 |
| Search: OpenSearch projection, pgvector | ADR-0006 | 3 |
| `access-service`, org units, fine-grained ACL | ARCHITECTURE.md §10 | 4 |
| `media-service` | ARCHITECTURE.md §5 | 5 |
| Helm chart, jib images, GitOps | ARCHITECTURE.md §13 | 6 |

Also accepted as skeleton deviations, recorded in `docs/CONTRACTS.md`: `config-service` accepts and
**ignores** `X-Tenant-Id` (one configuration per installation), and it opens a cold-start `draft-1`
because there is no create-draft endpoint.

---

## 9. Conventions — enforced, not aspirational

1. **Tests first.** No production class exists before a failing test names it.
2. **The domain imports nothing** — no Spring, JPA, Jackson or HTTP under `domain/`. ArchUnit fails
   the build.
3. **No domain vocabulary in framework code.** The framework knows `RecordType`, `Field`,
   `Relation` — never `Artist`, `Painting`, `Museum`. Customer field names (`inventoryNumber`,
   `title`, …) appear **only** in test fixtures and configuration data. The web test suite has a
   guard that scans its own sources and fails if a domain field name appears; it caught two real
   leaks while being written.
4. **One writer per aggregate.** No shared databases.
5. **The BFF may read from many services; it may write to at most one.** Multi-service writes are
   sagas owned by a domain service.
6. **Hibernate, not EclipseLink** — Maven Enforcer bans `org.eclipse.persistence:*`.
7. Every service needs a **context-loads `@SpringBootTest`** (§6.1).
8. British-English spelling in prose, to match the existing documents.

---

## 10. How this was built, and how to resume it

Phase 0 was built by **three parallel Claude Code agents in tmux windows**, one per component,
coordinated through two documents: `.agents/SHARED-BRIEF.md` (environment facts and conventions)
and `docs/CONTRACTS.md` (frozen wire shapes). Their briefs and completion reports are in
`.agents/`.

```bash
./.agents/launch-tmux.sh --detach   # create the session
./.agents/open-windows.sh           # one iTerm window per agent
tmux kill-session -t mpria          # tear down
```

Gotchas, if you run the team again: grouped tmux sessions are required or all terminals mirror one
window; AppleScript-launched terminals need the **absolute** `tmux` path; the folder-trust dialog
swallows the initial prompt, so send `Enter` first and then a one-line "read your brief" message.

**That cross-checking is what found the bugs in §6** — the renderer caught the `hidden` leak, the
BFF caught catalog-service being unbootable. Write the shared brief and the contract first; that is
what makes agents able to check each other rather than merely work in parallel.

### Suggested next steps

1. Phase 1 cataloguing core: relations + recursive CTE traversal, the full field-type catalogue.
2. Replace token relay with client-credentials + audience restriction.
3. `ConfigurationPublished` event via the Modulith outbox, wired to the schema-provider cache that
   is already stubbed for it.
4. Design the **extension-point replacement for Groovy** — ADR-0005 removes it but the replacement
   (restricted expression language + webhooks) is still only sketched. This is the biggest open
   design question, and it is a product surface, not an implementation detail.
