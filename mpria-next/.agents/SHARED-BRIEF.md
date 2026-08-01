# Shared brief — read this first

You are one of three agents building the Phase-0 walking skeleton of **MpRIA Next**, a greenfield
successor to the MpRIA Vaadin 8 / Payara 5 monolith. Project root:
`/Users/tushartari/development/mpria/mpria-next`.

The other two agents are working **concurrently in the same repository**. Stay strictly inside
your own directory. Do not edit the root `pom.xml`, `services/pm-contract/`,
`services/catalog-service/`, `platform/`, or any file outside your assignment. If you believe
something outside your directory must change, stop and report it instead of changing it.

## Read before writing anything

| File | Why |
| --- | --- |
| `docs/CONTRACTS.md` | **Binding** inter-service wire contracts. `catalog-service` is already built and already consumes some of these — a rename breaks a shipped service. |
| `ARCHITECTURE.md` | §5 bounded contexts, §6 layering, §7 the PresentationModel contract, §10 auth, §14 testing |
| `docs/ui-configuration.md` | The configuration/layout design (essential for config-service and web) |
| `services/catalog-service/src/main/java/**` | **The reference implementation.** Mirror its package layout, naming, javadoc density and test style. Read `ArchitectureTest.java`, `CatalogRecordService.java`, `RecordSchema.java`, `SecurityConfig.java`, `CatalogExceptionHandler.java`. |
| `services/pm-contract/src/main/java/com/zetcom/mpria/pm/**` | The PresentationModel types. Especially `Span.java` (breakpoint cascade) and `LayoutNode.java` (sealed hierarchy). |

`catalog-service` currently passes **38 unit tests + 7 Testcontainers integration tests**. That is
the bar for rigour.

## Build commands — batch mode, always

```bash
cd /Users/tushartari/development/mpria/mpria-next
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
mvn -B -s .mvn/settings-central.xml -pl services/<your-module> test
```

`-s .mvn/settings-central.xml` is required: the machine's `~/.m2/settings.xml` mirrors everything
through `nexus.zetcom.group`, which is unreachable off-VPN. `-B` (batch mode) is required — no
interactive prompts, no download progress spam.

## Environment facts already paid for. Do not rediscover these.

- Java 21, Spring Boot **4.1.0**, Spring Framework 7, Spring Security 7, Hibernate ORM 7.4.1.
- **Jackson 3**: `tools.jackson.databind.*`. Annotations stay `com.fasterxml.jackson.annotation.*`.
- **Boot 4 split the test slices** out of `spring-boot-test-autoconfigure` into per-module starters:
  - `@WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, needs
    `spring-boot-starter-webmvc-test`
  - `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, needs
    `spring-boot-starter-data-jpa-test`
  - `@AutoConfigureTestDatabase` → `org.springframework.boot.jdbc.test.autoconfigure.*`
  - `jwt()` post-processor → `spring-boot-starter-security-test`
  - `spring-boot-starter-test` alone provides **none** of these.
- `@MockBean` is gone → use `@MockitoBean`
  (`org.springframework.test.context.bean.override.mockito.MockitoBean`).
- `HttpStatus.UNPROCESSABLE_ENTITY` is deprecated → `UNPROCESSABLE_CONTENT`.
- Testcontainers 2: artifacts are `testcontainers-<module>`; `PostgreSQLContainer` is **not** generic.
- A `@WebMvcTest` that picks up the resource-server filter chain needs a `@MockitoBean JwtDecoder`.
- Keycloak's realm importer rejects unknown JSON keys outright (learned by breaking it).

## Non-negotiable conventions

1. **Tests first.** No production class before a failing test names it.
2. **The domain imports nothing** — no Spring, JPA, Jackson or HTTP under `domain/`. Copy
   `ArchitectureTest` from catalog-service and adapt the package; it must pass.
3. **No domain vocabulary in framework code.** The framework knows `RecordType`, `Field`,
   `Relation` — never `Artist`, `Painting`, `Museum`. Customer field names belong only in test
   fixtures and configuration data.
4. Explanatory javadoc/comments saying **why**, not what. No TODOs, no placeholders, no unused code.
5. British-English spelling in prose, to match the existing documents.
6. Iterate against **real** compiler and test output. Never assume an API exists — check the jar or
   compile.

## Definition of done

Your build is green, with no compiler warnings, run by you and observed — not assumed.

Then report: what you implemented, real test counts, anything in `docs/CONTRACTS.md` you could not
honour and why, and any Boot 4 surprises worth propagating to the others.

Work autonomously through to completion. Do not pause for confirmation.
