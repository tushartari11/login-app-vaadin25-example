---
name: new-bounded-context
description: >
  Use this skill when adding a new service (bounded context) to MpRIA Next, or when adding a new
  layer/port/adapter to an existing one. Triggers on: "add a new service", "create a bounded
  context", "scaffold a service", "new microservice", "start the search-service", "add
  media-service", "add access-service", "set up a new module", or any request to create a new
  deployable unit or a new outbound port. Follow it end to end — the checklist exists because
  scaffolding by hand has already shipped a service that could not start.
---

# New Bounded Context

Scaffolding a service here is a checklist, not a judgement call. Doing it ad hoc has already
produced a service that **compiled, passed 48 unit tests and 7 integration tests, and could not
start** — because Spring Boot 4 moved `RestClient.Builder`'s autoconfiguration into a starter that
was not on the classpath, and no test loaded the full context.

Work through every section. Skipping the `@SpringBootTest` is the one that costs you.

---

## Step 0 — Justify the boundary before creating it

A new service must own **disjoint data** and have a **different rate of change or scaling axis**.
If it shares a transaction boundary with an existing context, it is a **module**, not a service.

> Relations did not get their own service precisely because a relation and the records it joins
> commit together. Splitting them would have manufactured a distributed transaction for nothing
> (ADR-0007).

Answer these before writing code:

- [ ] Which tables does it **write**? Does any other service write them? (Must be no)
- [ ] What is its transaction boundary?
- [ ] What happens to the rest of the platform when it is **down**?
- [ ] Does it scale on a different axis from its neighbours?
- [ ] Could it be a Spring Modulith module inside an existing service instead?

If the honest answer to the last question is yes, do that. Merging later is cheap; unpicking a
network call is not.

---

## Step 1 — Module and pom

Create `services/<name>-service/` and add it to the root `pom.xml` `<modules>`.

Start from **`services/catalog-service/pom.xml`** — it is the reference. Take:

- `spring-boot-starter-web`, `-validation`, `-oauth2-resource-server`, `-actuator`
- **`spring-boot-starter-restclient`** if the service calls another over HTTP. This is the one that
  bites: it is *not* transitive from `-web` in Boot 4
- `spring-boot-starter-data-jpa` + `flyway-core` + `flyway-database-postgresql` + `postgresql`
  **only if it owns data**. A stateless service must not have a datasource
- Test scope: `spring-boot-starter-test`, **`-webmvc-test`**, **`-security-test`**,
  **`-data-jpa-test`** (if JPA), `spring-boot-testcontainers`, `testcontainers-postgresql`,
  `testcontainers-junit-jupiter`, `archunit-junit5`

Boot 4 split the test slices into per-module starters — `spring-boot-starter-test` provides
**none** of `@WebMvcTest`, `@DataJpaTest`, or `jwt()`.

## Step 2 — Package structure

```
com.zetcom.mpria.<context>
├── <Name>ServiceApplication.java   @SpringBootApplication (+ @EnableConfigurationProperties)
├── domain/            pure. imports NOTHING — no Spring, JPA, Jackson, HTTP
├── application/       use cases, @Transactional
│   └── port/          outbound port interfaces
├── infrastructure/
│   ├── persistence/   *Entity, Spring Data repo, port adapter
│   ├── security/      SecurityConfig
│   └── <peer>client/  RestClient adapters for other services
└── api/               @RestController, DTOs, @RestControllerAdvice
```

**Do not put adapter `@Bean` wiring on the application class.** Every `@WebMvcTest` instantiates
those beans while filtering out the `@Component`s they depend on, so every slice test fails with
`UnsatisfiedDependencyException`. Put it in a separate `@Configuration`. A dependency-free bean
(like `Clock`) is fine on the application class.

## Step 3 — The mandatory tests

Write these **before** the production code they cover.

- [ ] **`ArchitectureTest`** — copy from `catalog-service`, adapt the package. Must include: layer
      dependency direction, the domain-imports-nothing rule, `*Entity` confined to
      `infrastructure/persistence/`, `@Transactional` only in `application/`, and no `System.out`
- [ ] **`<Name>ServiceApplicationIT`** — a `@SpringBootTest` that asserts the context starts and
      every port is bound to a real adapter. **Non-negotiable.** Use `@MockitoBean JwtDecoder` so
      it does not need a live issuer, and Testcontainers if it owns data
- [ ] **Domain unit tests** — invariants, no Spring context
- [ ] **Use-case tests against fakes** — the fake must enforce real invariants (tenant scoping,
      optimistic locking), not just return what the test asks for
- [ ] **`@WebMvcTest`** per controller, including an unauthenticated 401 and any role-gated 403.
      Remember `jwt()` does **not** run your `JwtAuthenticationConverter` — pass `.authorities(...)`
- [ ] **Testcontainers IT** if it owns data — real Postgres, real Flyway, real `jsonb`

## Step 4 — Persistence (only if it owns data)

- [ ] Flyway migration under `src/main/resources/db/migration/V1__<table>.sql`
- [ ] Spine columns on every table: `id`, `tenant_id`, audit columns, `version`
- [ ] `tenant_id` **even though the MVP is single-tenant** — retrofitting it later is the migration
      nobody wants
- [ ] `spring.jpa.hibernate.ddl-auto: validate`. Flyway owns DDL; a rolling deploy must not race on it
- [ ] Every repository finder tenant-scoped **by parameter**
- [ ] Indexes for the query shapes you actually added

## Step 5 — Security

Copy `SecurityConfig` from `catalog-service`: resource server, JWT, stateless sessions, actuator
health/info permitted, everything else authenticated, CSRF disabled (no cookie auth reaches it).

- [ ] Tenant/org unit derived from **token claims only** — copy the `JwtPrincipals` pattern
- [ ] A missing claim is a 403 problem document, not a 500
- [ ] Role gating, if any, has a test that actually exercises it

## Step 6 — Configuration and platform

- [ ] `application.yaml` modelled on `catalog-service`: port, OIDC issuer
      `${OIDC_ISSUER_URI:http://localhost:8180/realms/mpria}`, actuator
      `health,info,metrics,prometheus`, `shutdown: graceful`,
      `spring.threads.virtual.enabled: true`
- [ ] Allocate a port and record it in `docs/CONTRACTS.md` and the README
- [ ] Add the service to `platform/compose/docker-compose.yml` under the `apps` profile
- [ ] Add its database to `platform/compose/postgres-init/01-create-databases.sql` if it owns data
- [ ] Add it to the Prometheus scrape list in `platform/compose/observability/prometheus.yml`

## Step 7 — Contract and documentation

- [ ] Add its endpoints to `docs/CONTRACTS.md` **before** consumers are written against it —
      including error statuses and their problem-type slugs
- [ ] Add it to `ARCHITECTURE.md` §5 with what it owns and its transaction boundary
- [ ] Update the README repository-layout block and, if relevant, the "not built yet" table
- [ ] Write an ADR if the boundary decision had a real rejected alternative
- [ ] Update `mpria-next-own-memory.md` §2 and §5

---

## Final verification — observe it, do not assume it

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
cd ~/development/mpria/mpria-next
mvn -B -s .mvn/settings-central.xml clean verify        # off-VPN; drop -s on VPN
```

Then confirm, explicitly:

- [ ] The reactor build passes with **no new compiler warnings**
- [ ] `ArchitectureTest` passes — and **fails when deliberately violated**. An architecture test
      that has never failed has not been shown to work
- [ ] The context-loads IT passes
- [ ] `docker compose --profile apps up -d --build` starts the new container healthy

Report real numbers. "Should work" is not a result.
