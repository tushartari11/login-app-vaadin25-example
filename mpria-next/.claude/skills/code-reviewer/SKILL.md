---
name: code-reviewer
description: >
  Use this skill when the user wants a code review of their changelist, diff, or recently
  modified files in MpRIA Next. Triggers on: "review my changes", "review this diff", "review my
  CL", "code review", "review this PR", "review my commit", "check my changes", "what did I miss",
  "review before merge", "pre-commit review", "changelist review", or any request to evaluate
  recent code changes against project standards. This skill focuses on change-level concerns:
  architectural boundaries, transaction and service ownership, contract compatibility, test level,
  and whether the change can actually start.
---

# Code Reviewer — MpRIA Next

Expert reviewer for the MpRIA Next stack (**Java 21 / Spring Boot 4.1 / Hibernate 7 / Next.js 15**).
The user is a Senior Java Architect — deliver precise, actionable comments at expert level, with no
hedging and no restating of the diff.

> **This is not `framework-vaadin8`.** Do not apply Java 8 rules, `javax.*` namespace rules, EJB
> conventions or Vaadin conventions. Java 21 language features are *encouraged* here.

Standards live in `ARCHITECTURE.md`, `docs/CONTRACTS.md`, `docs/adr/`, and
`mpria-next-own-memory.md`. Read the relevant one before asserting a rule.

---

## Review Process

1. **Identify changed files** — `git status` / `git diff`, or the files the user names
2. **Determine intent** — feature, bug fix, refactor, contract change, or infrastructure
3. **Determine blast radius** — which bounded context, and **which other services consume it**
4. **Review against the checklists below**, in order — Checklist 1 catches the expensive mistakes
5. **Check for missing pieces** — the right test level, contract updates, ADR updates

---

## Output Format

```
## Code Review Report

**Change scope:** [what changed]
**Change type:** [feature / fix / refactor / contract / infra]
**Services affected:** [and any downstream consumers]
**Files reviewed:** [list]

---

### Blocking Issues (must fix before merge)
### Suggestions (should fix)
### Nits (minor, optional)
### Positive Observations (brief — only genuinely good decisions)

---

### Missing Items Checklist
```

Each comment:

```
**[CATEGORY]** File:line — Short title
- Issue: what is wrong, and what it breaks
- Suggestion: the concrete fix
```

Categories: `ARCHITECTURE`, `CONTRACT`, `TRANSACTION`, `SECURITY`, `TESTING`, `BOOT4`,
`PERSISTENCE`, `RENDERER`, `STYLE`.

---

## Checklist 1 — The expensive mistakes

These are the ones a green test suite does not catch. Check them **first**.

- [ ] **Can the application still start?** If the change adds a `@Component`/`@Service` with a new
      injected dependency, is the starter that *provides* that bean actually on the classpath?
      Boot 4 moved autoconfiguration into per-module starters — `RestClient.Builder` needs
      `spring-boot-starter-restclient`, not `spring-boot-starter-web`. **Is there a
      `@SpringBootTest` covering it?** A missing one let a completely unbootable service ship with
      48 green unit tests
- [ ] **Contract compatibility** — does the change alter a request/response shape in
      `docs/CONTRACTS.md`? If so, is the document updated, and is **every consumer** updated in the
      same changelist? A field rename here silently breaks a built service
- [ ] **Transaction boundary is one aggregate** — no second aggregate, no second service, joined
      into one `@Transactional`
- [ ] **The BFF writes to at most one service per request.** A multi-service write must be a saga
      owned by a domain service
- [ ] **Optimistic locking is genuinely enforced** — a version compared against itself is not a
      check. Compare the domain object's version, not just the managed entity's
- [ ] **`hidden` is not used as confidentiality.** A field hidden for authorisation reasons must
      have `value: null`; the flag alone still ships the value to the browser

## Checklist 2 — Architecture and layering

- [ ] `domain/` imports nothing framework — no Spring, JPA, Jackson, HTTP, servlet
- [ ] Dependency direction: `domain` ← `application` ← {`infrastructure`, `api`}
- [ ] `@Transactional` appears only in `application/`
- [ ] Controllers depend on use cases, never on adapters or repositories
- [ ] `*Entity` classes stay inside `infrastructure/persistence/`
- [ ] Ports declared in `application/port/`, with both a real adapter and a test fake
- [ ] `ArchitectureTest` still passes, and was **extended** if the change introduces a new rule-worthy boundary
- [ ] One writer per table — no service writing another's tables
- [ ] **No domain vocabulary in framework code.** `RecordType`, `Field`, `Relation` — never
      `Artist`, `Painting`, `Museum`. Customer field names belong only in fixtures and configuration

## Checklist 3 — Java 21 / Spring Boot 4

- [ ] Constructor injection — no field injection, no `@Autowired` on fields
- [ ] Records for DTOs and value objects; sealed interfaces where the hierarchy is closed
- [ ] `switch` over a sealed type is exhaustive without a `default` (so adding a case fails to compile)
- [ ] No self-invocation of `@Transactional` / proxied methods (`this.method()` bypasses the proxy)
- [ ] SLF4J with parameterised messages — no string concatenation, no `System.out`
- [ ] Jackson 3 imports are `tools.jackson.*`; annotations remain `com.fasterxml.jackson.annotation.*`
- [ ] `HttpStatus.UNPROCESSABLE_CONTENT`, not the deprecated `UNPROCESSABLE_ENTITY`
- [ ] Adapter `@Bean` wiring is **not** on the `@SpringBootApplication` class (it breaks every `@WebMvcTest`)
- [ ] Immutable collections returned from getters; `Collections.unmodifiableMap(new LinkedHashMap<>())`
      where serialised **order** matters (`Map.copyOf` does not preserve it)
- [ ] Compiles with no new warnings under `-Xlint:all` (watch `this-escape`)

## Checklist 4 — Persistence

- [ ] Flyway migration added for any schema change; `ddl-auto` stays `validate`
- [ ] Migration is forward-only and safe under a rolling deploy (two app versions live at once)
- [ ] New tables carry `tenant_id`, audit columns, and `version`
- [ ] Every finder is tenant-scoped **by parameter**, not by ambient state
- [ ] `jsonb` payload changes respect the configured-schema validator — no unvalidated writes
- [ ] Indexes considered for new query shapes; recursive CTEs have an explicit depth bound

## Checklist 5 — Security

- [ ] Tenant and org unit come from **token claims only**, never a parameter or header
- [ ] Coarse authorisation in the filter chain; record-level authorisation in the application layer
- [ ] New endpoints are authenticated by default — check the `authorizeHttpRequests` chain
- [ ] Role gating is actually exercised by a test. **`jwt()` does not run your
      `JwtAuthenticationConverter`** — without explicit `.authorities(...)`, the test proves nothing
- [ ] No secrets in code, config, logs or test fixtures
- [ ] Errors are RFC 9457 problem documents and leak no internals

## Checklist 6 — Testing

Not "are there tests" but **is the test at the right level**:

- [ ] Domain rule → a domain unit test, no Spring context
- [ ] Orchestration / tenant isolation / concurrency → application test against **fakes**
- [ ] Anything touching `jsonb`, SQL or Flyway → Testcontainers IT, not a mock
- [ ] Status codes, ETags, problem documents → `@WebMvcTest`
- [ ] New bean wiring → the context-loads `@SpringBootTest`
- [ ] Renderer behaviour → Vitest, **with no domain field name anywhere in the test**
- [ ] Test names state the behaviour, not the method under test
- [ ] Fakes enforce real invariants; a stub that returns whatever the test asks proves nothing

## Checklist 7 — Frontend (`web/`)

- [ ] TypeScript `strict`; no `any` — `unknown` plus narrowing
- [ ] No domain knowledge in the renderer: no field names, no record types, no special cases
- [ ] Layout is CSS Grid + flex + container queries. **No absolute positioning** — that was v8's raster layout
- [ ] Breakpoint handling cascades and is resolved in CSS, not from `window` size (must be correct pre-hydration)
- [ ] a11y: real `<label for>`, `aria-invalid`, `aria-describedby`, `aria-required`
- [ ] Actions come from `actions[]`; the client never constructs a URL
- [ ] Tokens handled server-side, never `localStorage`

## Checklist 8 — Documentation

- [ ] `docs/CONTRACTS.md` updated for any wire-shape change
- [ ] A new ADR for any decision with a real rejected alternative — or an existing ADR amended if this contradicts it
- [ ] README "not built yet" table still accurate
- [ ] `mpria-next-own-memory.md` updated if a new environment trap was discovered
- [ ] Comments explain **why**, not what; no TODOs left behind

---

## Review Priorities

**Blocking:** cannot start · breaks a contract without updating consumers · crosses a layer
boundary · widens a transaction beyond one aggregate · tenant or authorisation defect · missing
migration · a test that asserts nothing.

**Suggestion:** wrong test level · missing ADR · unclear naming · a fake that should enforce an
invariant · duplication that will drift.

**Nit:** formatting, comment wording, import order.

Be specific about *why* something is blocking — name the failure it causes, not the rule it breaks.
If the change is clean, say so briefly rather than manufacturing findings.
