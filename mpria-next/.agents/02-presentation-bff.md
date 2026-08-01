# Agent 2 — presentation-bff

Read `.agents/SHARED-BRIEF.md` first. It contains the build commands, the Boot 4 gotchas and the
conventions. Everything below is specific to your assignment.

**Your directory: `services/presentation-bff/` only.** `services/config-service/` and `web/` belong
to other agents running right now. `config-service` may not exist yet — you talk to it over HTTP
and your tests use fakes, so your build must be green regardless.

Read `ARCHITECTURE.md` §7 and ADR-0003 for why this layer exists at all, and **all** of
`services/pm-contract/src/main/java/com/zetcom/mpria/pm/` — that is your output type.

## What this service is

The presentation-model layer, in Java, emitting JSON. It is what makes the frontend replaceable: a
client implements `render(PresentationModel)` and needs no knowledge of record types or business
rules. It owns no data, holds no session, and writes to at most one service per request.

## Build

1. Package root `com.zetcom.mpria.bff`, layered `domain/ application/ infrastructure/ api/`.
   Copy `ArchitectureTest` from catalog-service and adapt. This service has no database — express
   that as a rule if you can (nothing may import `jakarta.persistence`).

2. **Outbound ports** in `application/port/`:
   - `ConfigurationGateway` — current revision, screen layout, record-type schema
   - `CatalogGateway` — get / create / update a record, returning either a record or field errors

   HTTP adapters in `infrastructure/` using `RestClient`, relaying the caller's bearer token from
   `SecurityContextHolder`. catalog-service's `ConfigServiceSchemaProvider` shows the exact
   pattern — copy it, **including** its honest javadoc that client-credentials with audience
   restriction is the Phase-1 target and token relay is skeleton scope.

3. **`PresentationModelAssembler`** in `application/` — the heart of the service. Pure
   orchestration and mapping: no HTTP, no Spring annotations if avoidable, so it is unit-testable
   with fakes. It implements the six assembly rules in `docs/CONTRACTS.md`. Specifically:
   - `fields` contains exactly the fields the layout binds — use `Layout.boundFieldIds()` — each
     merging schema metadata (type, label, required, allowedValues) with the record's value.
   - `actions`: `save` always; `publish` enabled only when lifecycle is `DRAFT`; `archive`
     disabled with reason `record.alreadyArchived` when already `ARCHIVED`, and `confirm = true`.
   - **A 422 from catalog-service is not an error response.** Catch it, map `errors[fieldId]` onto
     `FieldState.errors`, and return **200** with the re-rendered model. The form must come back
     with messages attached to the right controls. This is the single most important behaviour in
     the service.
   - `meta.etag` mirrors the record's ETag — it is the next write's `If-Match`.
   - Never construct a client URL anywhere except `actions[].href`.

   `PresentationModel`'s constructor enforces that every layout-bound field has a `FieldState`.
   Let it fail loudly; do not work around it.

4. **API** — the four endpoints under "presentation-bff" in `docs/CONTRACTS.md`, returning
   `PresentationModel` directly.

5. **`application.yaml`** — port 8080, OIDC issuer
   `${OIDC_ISSUER_URI:http://localhost:8180/realms/mpria}`,
   `mpria.config-service.base-url` `${CONFIG_SERVICE_URL:http://localhost:8082}`,
   `mpria.catalog-service.base-url` `${CATALOG_SERVICE_URL:http://localhost:8081}`,
   actuator, graceful shutdown, virtual threads on. Mirror catalog-service's file.

6. **pom.xml** — it exists and already depends on `pm-contract` (installed in the local repo). Add
   `spring-boot-starter-webmvc-test` and `spring-boot-starter-security-test` (test scope); you may
   drop the redundant `org.springframework.security:spring-security-test`.

   For stubbing outbound HTTP prefer `RestClient` + `MockRestServiceServer`, or hand-written fakes
   behind your ports. Do not add WireMock unless the simpler option genuinely fails.

## Tests (substantive)

- assembler unit tests with fake gateways: populated form, blank `new` form, every
  action-enablement branch, and the 422 → `FieldState.errors` mapping (assert the message lands on
  the right field id **and** the response is 200)
- a layout binding a field the schema does not define fails loudly rather than producing a
  half-built model
- the returned `meta.etag` is the record's ETag
- `@WebMvcTest` with `jwt()`, including 401 when unauthenticated
- a JSON assertion that the emitted PM matches `docs/CONTRACTS.md`: `kind` discriminators present,
  nulls omitted, breakpoint keys plain
- the ArchUnit suite

## Done

`mvn -B -s .mvn/settings-central.xml -pl services/presentation-bff test` green, no warnings.
