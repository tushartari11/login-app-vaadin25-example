---
name: add-component-type
description: >
  Use this skill when adding a new UI component type or a new field type to MpRIA Next — for
  example a colour picker, a vocabulary lookup, a relation table, or a new FieldType constant.
  Triggers on: "add a component type", "add a field type", "new widget", "support a date range",
  "add a picker", "the renderer needs a", "add to the component registry", "new FieldType", or any
  request that would make the configuration palette or the renderer able to do something new.
  Follow it end to end — this change is cross-cutting and easy to half-complete.
---

# Add a Component or Field Type

This is the most cross-cutting change in the codebase. It touches **four places in two languages**,
and a half-done version fails at *configuration* time rather than compile time — an administrator
publishes a layout and the renderer shows a fallback, or the publish gate rejects something that
should be legal.

Decide first which of the two you are adding — they are different jobs:

| | **Field type** | **Component type** |
| --- | --- | --- |
| What it is | a new kind of *data* (`FieldType` enum) | a new way of *drawing* existing data |
| Cost | high — validation, storage, search, both enums | moderate — registry entry plus a renderer component |
| Example | `DATE_RANGE`, `GEO_POINT` | `RADIO_GROUP` for an existing `ENUM` |

**Prefer a component type.** Every field type costs a validator, a registry entry, a renderer
component, an index strategy, and a constant that must match across two services. `FieldType`'s
javadoc says it deliberately: anything expressible as a constrained `STRING` should be one.

---

## Part A — Adding a component type (the common case)

### A1. `config-service` — the registry

`ComponentRegistry` is the contract between the palette and every renderer.

- [ ] Add a `ComponentDefinition`: `type`, `label`, `acceptsFieldTypes`, `options`
- [ ] `acceptsFieldTypes` is what makes drag-and-drop safe — the designer refuses an invalid drop
      before an invalid configuration can exist. An unbound component (a gallery, a separator) has
      an empty list
- [ ] Add a test asserting the new entry is present and its `acceptsFieldTypes` are types that
      actually exist in `FieldType`

### A2. Publish validation still holds

The validator rejects a component type absent from the registry, and a field bound to a component
whose `acceptsFieldTypes` excludes it.

- [ ] Add a test that a layout using the new type **publishes successfully**
- [ ] Add a test that binding it to an incompatible field type is **rejected with a named error**

### A3. `web/` — the renderer

- [ ] Implement the component and register it in the component registry (`Record<string, FieldComponent>`)
- [ ] Apply `readOnly`, `required`, `hidden`, `errors` **mechanically** — the renderer must not
      know *why* a field is read-only
- [ ] a11y: real `<label for>`, `aria-invalid`, `aria-describedby`, `aria-required`
- [ ] Layout with grid/flex/container queries. **No absolute positioning**
- [ ] Behave correctly in server-rendered HTML before hydration

### A4. Contract and docs

- [ ] Add the type to the component-registry example in `docs/CONTRACTS.md`
- [ ] Document any `options` it accepts, with types and defaults

### A5. Tests

- [ ] Renderer test: renders, and honours each field-state flag
- [ ] Renderer test: still falls back gracefully for an *unknown* type (do not break the fallback)
- [ ] **No domain field name in any web test.** Use `fieldA`, `typeX`, `screen-1`. A guard test
      scans the sources and will fail you — it has already caught two real leaks

---

## Part B — Adding a field type (do this only when genuinely new data)

Everything in Part A, **plus** all of the following. Skipping any one leaves a runtime failure.

### B1. Both `FieldType` enums must match — exactly

There are two, in different services, and they are coupled by a string on the wire:

- `com.zetcom.mpria.catalog.domain.FieldType`
- `com.zetcom.mpria.config.domain.FieldType`

`catalog-service` calls **`FieldType.valueOf(type)`** on the value `config-service` sends. A
constant present in one and not the other is a **runtime failure in a different service** than the
one you edited.

- [ ] Add the constant to **both**, with identical spelling
- [ ] `config-service` has a test pinning the seven constants — update it deliberately, and read
      why it exists before changing it

### B2. `catalog-service` — validation

- [ ] Implement `reject(Object value)` for the new constant. Return a message that reads as a
      sentence completing "must be …", because that is how it surfaces to a cataloguer
- [ ] Decide how it arrives over JSON. Numbers arrive as `Number`; dates as `String`. Handle the
      widened forms, as `INTEGER` does for `Integer`/`Long`/whole `Double`
- [ ] Extend `FieldDefinition.violations` if the type needs a constraint beyond `maxLength` and
      `allowedValues`
- [ ] Tests: valid values, each invalid form, and that a type error suppresses downstream
      constraint noise (the existing pattern — do not report "at most 3 characters" for a number)

### B3. Persistence and search

- [ ] Confirm the value round-trips through `jsonb` with its type intact — **add a Testcontainers
      assertion**, not a unit test. H2 would happily pass what Postgres fails
- [ ] Consider whether it needs a generated column plus index, or an OpenSearch mapping later
      (Phase 3). Note it rather than building it

### B4. Contract and docs

- [ ] Add the constant to the `type` enumeration list in `docs/CONTRACTS.md`
- [ ] Update the TypeScript union in `web/src/lib/pm/types.ts`
- [ ] Map it to a default component in the registry, so a newly configured field is drawable

---

## Verification

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
cd ~/development/mpria/mpria-next
mvn -B -s .mvn/settings-central.xml clean verify        # off-VPN; drop -s on VPN
cd web && npm run test && npm run build
```

Then the end-to-end check that actually proves it — configuration is not code, so compiling is not
evidence:

- [ ] Add a field or slot using the new type to `config-service`'s seed revision
- [ ] Confirm the seed drift test still passes (bound field ids **equal** the record type's field ids)
- [ ] Start the stack and load `/demo` or a real screen; confirm it draws and round-trips a save

---

## Output

State plainly which files changed, in which of the four places, and what you verified end to end.
If you added a `FieldType`, say explicitly that **both** enums were updated — that is the one an
otherwise-complete change forgets.
