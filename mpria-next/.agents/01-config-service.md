# Agent 1 — config-service

Read `.agents/SHARED-BRIEF.md` first. It contains the build commands, the Boot 4 gotchas and the
conventions. Everything below is specific to your assignment.

**Your directory: `services/config-service/` only.** `services/presentation-bff/` and `web/` belong
to other agents running right now.

Also read `docs/ui-configuration.md` in full — it is the design document for this service.

## What this service is

The administration application: the replacement for the old ULC configuration tool. It owns record
types, field definitions, screen layouts and immutable configuration revisions. It is
**independently startable** and may be down while the rest of the platform runs, because consumers
pin to a published revision and cache it.

## Build

1. Package root `com.zetcom.mpria.config`, layered `domain/ application/ infrastructure/ api/`.
   Copy `ArchitectureTest` from catalog-service and adapt the package.

2. **Domain**
   - `ConfigurationRevision` — id, status `DRAFT | VALIDATED | PUBLISHED`, publishedAt. Immutable.
   - `RecordTypeDefinition` — recordType, schemaVersion, titleFieldId, `List<FieldDefinition>`.
   - `FieldDefinition` — id, type, label, required, maxLength, allowedValues.
   - `FieldType` — exactly the seven constants `STRING TEXT INTEGER DECIMAL BOOLEAN DATE ENUM`.
     catalog-service calls `FieldType.valueOf(type)` on your wire value, so a mismatch breaks a
     built service.
   - `ScreenLayout` — screenId, recordType, and a `com.zetcom.mpria.pm.Layout`.
   - `ComponentRegistry` — the entries from `docs/CONTRACTS.md`, each with `acceptsFieldTypes`.

   **The behaviour to test hardest is publish-time validation.** Publishing must reject:
   - a layout binding a field its record type does not define (name the offending field ids),
   - a component `type` absent from the registry,
   - a component bound to a field whose `FieldType` is not in that component's `acceptsFieldTypes`.

   That gate is the entire point of the service. An SME must get a precise, actionable error, never
   a stack trace.

3. **Storage: none.** No database in the skeleton. Seed the published revision from a classpath
   resource `src/main/resources/config/seed-revision.json`, loaded at startup into an in-memory
   repository behind a `ConfigurationRepository` port. State this plainly in the class javadoc and
   in a short `services/config-service/README.md` as deliberate skeleton scope, with Postgres +
   Flyway as Phase 2. Do **not** add a datasource dependency.

   The seed must define:
   - record type `object` with exactly the fields in `docs/CONTRACTS.md` — `inventoryNumber`
     (STRING, required, maxLength 50), `title` (STRING, required, maxLength 255), `description`
     (TEXT), `acquiredOn` (DATE), `condition` (ENUM: GOOD/FAIR/POOR); `titleFieldId` = `title`;
   - screen `object-detail` with the two-group responsive layout from `docs/CONTRACTS.md`, which is
     the same shape as `LayoutFixtures.twoGroupScreen()` in pm-contract's tests.

   The whole end-to-end demo depends on this seed being exactly right.

4. **API** — every endpoint under "config-service" in `docs/CONTRACTS.md`. Admin endpoints gated on
   realm role `mpria-config-admin`; publish additionally on `mpria-config-publisher`. Read
   endpoints need only an authenticated principal. RFC 9457 `ProblemDetail` for errors, exactly as
   `CatalogExceptionHandler` does it.

5. **`application.yaml`** — port 8082, OIDC issuer
   `${OIDC_ISSUER_URI:http://localhost:8180/realms/mpria}`, actuator health/info/metrics/prometheus,
   graceful shutdown, virtual threads on. Mirror catalog-service's file.

6. **pom.xml** — it exists. Add `spring-boot-starter-webmvc-test` and
   `spring-boot-starter-security-test` (test scope); you may drop the now-redundant
   `org.springframework.security:spring-security-test`. Keep everything else.

## Tests (substantive, not smoke tests)

- domain unit tests for each publish-validation rejection above, asserting the specific message
- a test proving the **seeded layout's bound field ids are exactly the seeded record type's field
  ids** — this is what protects the end-to-end demo from silent drift
- `@WebMvcTest` per controller with `jwt()`, including a 403 for a principal lacking
  `mpria-config-admin`
- a JSON test pinning the schema endpoint's exact wire shape against `docs/CONTRACTS.md`
- the ArchUnit suite

## Done

`mvn -B -s .mvn/settings-central.xml -pl services/config-service test` green, no warnings.
