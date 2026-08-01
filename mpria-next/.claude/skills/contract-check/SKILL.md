---
name: contract-check
description: >
  Use this skill when a change touches an inter-service API, a DTO, the PresentationModel shape, or
  docs/CONTRACTS.md itself — to verify producer and every consumer still agree. Triggers on:
  "check the contract", "contract check", "did I break the contract", "is this API change safe",
  "verify the wire format", "does the client still match", "I changed a DTO", "I renamed a field",
  "update CONTRACTS.md", "add an endpoint", or any change to a @RestController, a request/response
  record, the pm-contract module, or the TypeScript PM types. Use it proactively before merging any
  change to a service boundary.
---

# Contract Check — producer and consumers must agree

`docs/CONTRACTS.md` is **binding**. Services here are built independently — often in parallel — so
a field rename that looks local breaks a service that is already shipped, at runtime, with no
compiler error and frequently no test failure.

> This is not hypothetical. The write body was once unspecified: the client posted `values`, the
> BFF read `data`. Every save would have written an **empty record with no error anywhere**. It was
> caught by a re-read, not by a test. This skill exists to make that catch systematic.

---

## The contract graph

Know who consumes what before assessing any change.

```
config-service ──schema──▶ catalog-service      (ConfigServiceSchemaProvider deserialises it)
config-service ──layout──▶ presentation-bff
catalog-service ─record──▶ presentation-bff
presentation-bff ──PM JSON──▶ web               (renderer + TypeScript types)
pm-contract ──────────────▶ config-service, presentation-bff, and (by shape) web
```

| Producer | Consumers | Where it is pinned |
| --- | --- | --- |
| `config-service` schema endpoint | `catalog-service` | `ConfigServiceSchemaProvider.SchemaPayload` / `FieldPayload` |
| `config-service` screen endpoint | `presentation-bff` | its `ConfigurationGateway` adapter |
| `catalog-service` record API | `presentation-bff` | its `CatalogGateway` adapter |
| `presentation-bff` PM JSON | `web` | `web/src/lib/pm/types.ts` + the renderer |
| `pm-contract` types | all three JVM services | compile-time |

**`FieldType` is a landmine.** `catalog-service` calls `FieldType.valueOf(type)` on the wire string.
Renaming or removing a constant in `config-service` fails at runtime in a *different* service.

---

## Process

### Step 1 — Classify the change

| Class | Meaning | Action |
| --- | --- | --- |
| **Additive** | new optional field, new endpoint, new enum constant *only consumers tolerate* | Safe. Update `CONTRACTS.md` anyway |
| **Breaking** | rename, removal, type change, required-ness change, status-code change | Producer **and every consumer** in one changelist |
| **Semantic** | shape unchanged, meaning changed (e.g. a field now nullable in a new case) | The most dangerous class — no compiler or schema catches it. Requires an explicit consumer review |

Semantic changes deserve the most suspicion. `hidden: true` shipping its value was a semantic
defect: the shape was correct and every test passed.

### Step 2 — Find every consumer, in code

Do not reason from memory. Search:

```bash
cd ~/development/mpria/mpria-next
grep -rn "<fieldName>" --include=*.java --include=*.ts --include=*.tsx \
     --include=*.json --include=*.md services/ web/src/ docs/ \
     | grep -v /target/ | grep -v /node_modules/
```

Check all four surfaces — a consumer can be any of them:

1. **Java DTOs** on the consuming side (gateway/adapter records)
2. **TypeScript types** in `web/src/lib/pm/types.ts`
3. **Test fixtures and JSON assertions** on both sides
4. **`docs/CONTRACTS.md`** itself, plus any seed data in `config-service/src/main/resources/config/`

### Step 3 — Verify against the document

For the affected endpoint, confirm the implementation matches `docs/CONTRACTS.md` on:

- [ ] Path, method, and required headers (`Authorization`, `X-Tenant-Id`, `If-Match`)
- [ ] Request body field **names** and types
- [ ] Response body field **names** and types
- [ ] Status codes, including the error cases — and their RFC 9457 `type` slugs
- [ ] Whether an absent value is `null`, an empty collection, or an omitted key.
      `@JsonInclude(NON_NULL)` drops `null` but **not** `[]`, so `errors` / `options` /
      `capabilities` arrive as empty arrays. A consumer treating absence as empty will disagree

### Step 4 — Check the invariants the document sets

These are contract terms, not implementation preferences:

- [ ] **Every layout-bound field has a `FieldState`** — enforced by `PresentationModel`'s constructor
- [ ] **A disabled action carries a `disabledReason`** — enforced by `ActionModel`
- [ ] **`hidden` for authorisation reasons ⇒ `value: null`.** `hidden` alone is a rendering hint
- [ ] **A 422 from catalog-service is not an error response** — the BFF maps it to `FieldState.errors`
      and returns **200**
- [ ] **412 propagates unchanged** — a stale copy is a protocol outcome, not a field error
- [ ] **The client never constructs a URL** — only `actions[].href`
- [ ] **Breakpoint spans cascade upward**, and `Span.resolve` (Java) and `span.ts` (client) must
      agree. They are tested against the same table — keep it that way

### Step 5 — Prove it, do not assert it

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
mvn -B -s .mvn/settings-central.xml clean verify     # off-VPN; drop -s on VPN
cd web && npm run test && npm run build
```

A JSON round-trip or wire-shape test on **both** sides is the only real evidence. If the change is
not covered by one, that gap is itself a finding — add the test.

### Step 6 — Update the document in the same change

`docs/CONTRACTS.md` is the source of truth, not a changelog written afterwards. When a deviation is
deliberate (skeleton scope), record it **as** a deviation with the reason — as the `X-Tenant-Id`
note does — rather than leaving the document quietly wrong.

---

## Output Format

```
## Contract Check

**Change:** [what moved]
**Producer:** [service]
**Classification:** additive / breaking / semantic

### Consumers found
| Consumer | Surface | Status |
|---|---|---|
| catalog-service | ConfigServiceSchemaProvider.FieldPayload | ✅ updated / ❌ NOT updated |

### Contract document
- [ ] docs/CONTRACTS.md updated / does not need updating (why)

### Invariants
[each affected invariant, with verdict]

### Verdict
SAFE TO MERGE  |  BREAKING — N consumer(s) must change  |  NEEDS A TEST

### Required follow-ups
[ordered, concrete]
```

Be decisive. "Might break" is not useful — go and look at the consumer.

---

## Notes

- `docs/CONTRACTS.md` is shared and has been edited concurrently by several agents. **Re-read it
  immediately before editing**; do not write from a copy you read earlier in the session.
- If the contract and the code disagree and you cannot tell which is right, the **consumers'
  behaviour** is the tiebreak — that is what is actually deployed.
- Propose contract changes to the user rather than making them unilaterally when another service
  would have to change to match.
