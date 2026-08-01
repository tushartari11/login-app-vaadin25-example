# Agent 2 — presentation-bff · completion report

> Assignment: [`02-presentation-bff.md`](02-presentation-bff.md) · Directory: `services/presentation-bff/`
> Date: 2026-08-01

**Status: done.** `mvn -B -s .mvn/settings-central.xml -pl services/presentation-bff clean test`
passes from the project root — **72 tests, 0 failures, 0 skipped, no compiler warnings** — observed
on a cleaned `target/`, not assumed. The only stderr line is a JVM class-sharing notice from the
surefire fork, unrelated to compilation.

---

## 1. What was built

32 main classes, 11 test classes, one `application.yaml`. Package root `com.zetcom.mpria.bff`,
layered `domain/ application/ infrastructure/ api/`.

**`domain/`** — framework-free. `Lifecycle`, `RecordSnapshot`, `RecordSchema`/`FieldDefinition`,
`ScreenDefinition`, `ConfigurationRevision`, `FieldErrors`, and seven failure types.

Two decisions worth recording:

* `FieldDefinition.type` is a `String`, not an enum. This service never validates a value —
  catalog-service does, against the same schema — so it only relays the type for the renderer to pick
  a component. An enum here would mean a newly configured field type could not ship without
  redeploying the BFF, which is the coupling "configuration over code" exists to prevent.
* `Lifecycle` models `allowsEditing()` and nothing else. The transition rules belong to
  catalog-service; copying its state machine into a second service is duplication that drifts.

**`application/`** — `ConfigurationGateway` and `CatalogGateway` ports; `RecordSubmission`;
`ScreenRoutes`; and `PresentationModelAssembler`, which carries no Spring annotation at all so its
tests construct it with two fakes and run in microseconds.

`ScreenRoutes` is the single author of this service's client-facing URL space, used by both the
emitted `actions[].href` and the controller's `@RequestMapping`. Two independent copies of
`/api/v1/screens` is how a renamed route ships a form whose save button 404s.

**`infrastructure/`** — `RestClient` adapters for both upstreams, `CallerCredentials`,
`SecurityConfig`, `AssemblerConfiguration`.

Both adapters use `exchange()` rather than `retrieve()`, for one reason: the default status handler
would turn the 422 into an exception, and the 422 is not a failure — it is the form coming back with
messages on it. Configuration lookups are pinned to a revision resolved once per request, so a
publish landing mid-request cannot yield a layout and a schema from different revisions; the cache
key is `(revisionId, tenantId, resourceId)`, because two customers on one revision have different
configurations.

**`api/`** — five endpoints returning `PresentationModel` directly, and `BffExceptionHandler`.
Notice what is *absent* from that handler: any branch for a validation failure. A record that does
not satisfy its schema is answered with 200 and a re-rendered form. A handler for it would mean the
form had already been lost.

### The six assembly rules

| Rule | Where | Notes |
| --- | --- | --- |
| 1. Resolve current revision, cache by it | `ConfigServiceGateway` | pinned per request, tenant in the key |
| 2. Fetch layout + schema | `PresentationModelAssembler.resolve` | both at the pinned revision |
| 3. Fetch the record, skip for `new` | `existingRecord` / `blankForm` | |
| 4. One `FieldState` per layout-bound field | `fieldStates` | via `Layout.boundFieldIds()`, deduplicated |
| 5. `actions` from lifecycle | `actions`/`publish`/`archive` | `save` always; `publish` only in `DRAFT`; `archive` disabled `record.alreadyArchived`, `confirm` always |
| 6. `meta.etag` mirrors the record's ETag | `meta` | asserted directly, and after a transition |

Beyond the rules as written: an archived record's fields come back `readOnly` with empty
`capabilities`; a blank form offers only `save`, because a record that does not exist has no
lifecycle to move; a disabled `publish` states `record.alreadyPublished` or `record.alreadyArchived`
rather than one vague key.

---

## 2. Tests — real counts

| Suite | Tests | What it pins |
| --- | --- | --- |
| `PresentationModelAssemblerTest` | 26 | the six rules, every action branch, 422 → `FieldState.errors`, transitions |
| `ScreenControllerTest` (`@WebMvcTest` + `jwt()`) | 11 | routing, `If-Match` relay, 401, problem statuses |
| `CatalogServiceGatewayTest` | 11 | 422 → rejection, 409 disambiguation, 404, `If-Match` on the wire |
| `ConfigServiceGatewayTest` | 9 | tenant header vs bearer, caching by revision, real `Layout` deserialisation |
| `ArchitectureTest` | 8 | layering, pure domain, no persistence, no HTTP in `application/` |
| `PresentationModelJsonContractTest` | 6 | `kind` discriminators, plain breakpoint keys, nulls omitted, round trip |
| `PresentationBffApplicationTest` | 1 | the real context refreshes with both gateways wired |
| **Total** | **72** | |

The 422 behaviour is covered at both levels on purpose: the adapter test proves the problem document
becomes a rejection *without throwing*, and the assembler test proves the message lands on the right
field id **and** that the outcome is a 200-shaped model with the user's typed values still in it.

---

## 3. Verification actually performed

* Clean `mvn … clean test` from the project root, exit 0, no `[WARNING]` lines, no `warning:` lines.
* **The `@Bean`-on-application-class fix was proved, not assumed.** After moving the wiring out, I
  temporarily put a `@Bean String` back on `PresentationBffApplication`, confirmed the new ArchUnit
  rule fails with exactly one violation, then reverted. Worth doing: earlier in this build I had
  written a session rule that named *classes* where ArchUnit expects *packages*, so it passed against
  nothing until I noticed and fixed it. A guardrail that cannot fail is worse than none.
* Every Spring and Jackson API used was checked against the jar with `javap` before being written —
  `RestClient.exchange`, `ConvertibleClientHttpResponse.bodyTo`,
  `MockRestServiceServer.bindTo(RestClient.Builder)`, `MockMvcTester`'s builder, `JsonNode`. Two
  guesses were caught this way: `MvcTestResultAssert` has no `doesNotHaveStatus`, and Jackson 3
  renamed `fieldNames()` to `propertyNames()`.
* The 400 for an unknown lifecycle state is asserted rather than assumed — I did not know whether
  Spring maps a failed path-variable enum conversion to 400 or 500 until the test said 400.

---

## 4. Contract items and how they were resolved

**1. `publish`/`archive` hrefs originally pointed at a route nothing served.** `ActionModel` requires
a non-blank href, and the assignment scoped the API to four endpoints and `CatalogGateway` to
get/create/update — so the actions were computed correctly but unreachable. Raised rather than
silently widened; then implemented on explicit instruction. `docs/CONTRACTS.md` now carries the fifth
row.

**2. The write request body was unspecified, then specified as `values` — and my DTO read `data`.**
Caught by re-reading `CONTRACTS.md` before editing it. Had this shipped, the web client would have
sent `values`, Jackson would have bound nothing, and every submitted form would have posted an
**empty payload to catalog-service and returned 200** — both sides looking correct in isolation.
`SubmissionRequest` now reads `values`. The client's `values` and catalog's `data` are deliberately
*not* reconciled: this service is where the two vocabularies meet, and renaming either end would make
one published contract wrong.

**3. catalog-service answers two different failures with 409.** An illegal transition
(`illegal-transition`) and a lost write race (`concurrent-modification`) share a status but need
different things from the user — "that move is not allowed" against "reload, somebody else won". The
adapter dispatches on the problem document's `type`, matching the last path segment so the docs host
can move. Unrecognised or absent type falls back to stale, because sending someone to re-read is the
safer wrong answer than telling them a permitted action is forbidden.

**4. `hidden: true` is not a redaction** (raised by agent 3, verified by them against the rendered
payload). No violation exists here — nothing in Phase 0 sets `hidden`, since there is no field-level
authorisation and fine-grained auth is `access-service`'s job. I deliberately did **not** build a
hiding mechanism nothing drives; that would be an untestable dead branch. The obligation is instead
recorded on `fieldState`, the one method a future change must edit, stating that setting the flag
alone leaks.

**Not honoured:** nothing else. No renames were needed, so nothing `catalog-service` or `config-service`
consumes was disturbed.

---

## 5. Worth propagating to the other two agents

**1. `spring-boot-starter-web` does not give you a `RestClient.Builder`.** Boot 4 modularised the
auto-configurations; `RestClientAutoConfiguration` lives in `spring-boot-starter-restclient`. Without
it, any adapter injecting `RestClient.Builder` fails at context refresh — and no slice test will tell
you, because `@WebMvcTest` never creates the adapter.

> **`catalog-service` has this latent failure.** `ConfigServiceSchemaProvider` injects
> `RestClient.Builder` and `services/catalog-service/pom.xml` has no such starter. Its build is green
> only because nothing there loads a context. One `@SpringBootTest` with a `@MockitoBean JwtDecoder`
> would surface it. I did not touch that module.

**2. `spring.http.client.*` is deprecated since Boot 4.0** in favour of **`spring.http.clients.*`**
(plural). Both keys are in the metadata; only the plural one is current. Timeouts are set once there
rather than per-adapter, so two upstream clients cannot drift into two ideas of patience.

**3. A `@Bean` on the `@SpringBootApplication` class is built by every test slice** — confirming
agent 1's finding. My slice test survived only by accident: it declares `@MockitoBean
PresentationModelAssembler`, which replaces the definition so the factory method never runs. The next
slice test that didn't happen to mock it would have failed. Wiring now lives in
`infrastructure/AssemblerConfiguration`, a plain `@Configuration`, which a slice excludes wholesale —
independently evidenced by `SecurityConfig` needing an explicit `@Import` to be visible at all.

**4. `MvcTestResultAssert` has only positive status assertions.** No `doesNotHaveStatus`; drop to
`…exchange().getResponse().getStatus()` for a negative check.

**5. Jackson 3: `JsonNode.fieldNames()` → `propertyNames()`**, returning `Collection<String>`.

**6. `@MockitoBean JwtDecoder` does satisfy `@ConditionalOnMissingBean`** — no JWKS fetch at startup,
confirmed in both the `@WebMvcTest` and the `@SpringBootTest`.

**7. `@JsonIgnoreProperties(ignoreUnknown = true)` on every inbound payload record.** Both upstreams
own their shapes and will grow them; a BFF that failed on an added audit field would couple two
deployments together for nothing.

---

## 6. Scope deliberately not built

* **No caching of records.** Records are mutable and this is a read-through view of somebody else's
  transactional core; a stale form is worse than a slow one.
* **Configuration cache is invalidated by nothing.** It should be evicted by the
  `ConfigurationPublished` event, which is why `evictAll()` exists. Until then a publish reaches a
  running BFF through `currentRevision()` changing, which changes every cache key anyway.
* **Token relay, not client credentials.** Outbound calls carry the caller's own token. The target is
  the client-credentials grant with a per-service client and an audience restriction
  (ARCHITECTURE.md §10), so a compromised BFF token cannot be replayed against config-service. It is
  confined to `CallerCredentials` so replacing it touches nothing else.
* **No `hidden`, no field-level authorisation** — see §4.4.
* **Enum option labels default to the option value.** config-service does not yet supply vocabulary
  labels, and inventing an i18n key here would be a convention nobody configured.
* **No bean validation on the write body.** The configured schema decides what is required and
  catalog-service enforces it; a structural check here that disagreed would reject a valid form before
  the real rules ever ran.

---

## 7. Files touched outside `services/presentation-bff/`

One, on explicit later instruction: **`docs/CONTRACTS.md`**. Three additions, all within the
presentation-bff section or the client-implementer notes —

1. the fifth endpoint row for the lifecycle transition, with its no-body/400 semantics;
2. the 409-versus-412 distinction the transition path made necessary;
3. the `hidden: true` is-not-secret line agent 3 asked for, pointing at their fuller section.

The shared brief said to propose changes to that file rather than make them, so it is called out here
rather than buried. It is shared and not under git in this checkout, and it changed under me once
already during this assignment — I re-read it immediately before editing, which is how the `values`
break in §4.2 was caught. Anyone rewriting that section should expect to carry these three additions
with it.
