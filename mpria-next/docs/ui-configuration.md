# UI Configuration — the admin application and the layout designer

> Companion to [`../ARCHITECTURE.md`](../ARCHITECTURE.md). Covers requirements **#5** (a Java,
> independently startable configuration application replacing the ULC tool) and **#8**
> (drag-and-drop layout configuration).

---

## 1. What "configurable UI" has to mean here

MpRIA's customers do not want a different application; they want *their* fields, in *their* order,
with *their* labels, grouped the way their cataloguing practice works. Historically that was
delivered by a ULC configuration client writing into the data dictionary, plus Groovy for anything
the tool could not express.

The target is that **an SME with no developer present** can:

1. define a record type and its fields,
2. arrange those fields on a form by dragging them onto a grid,
3. set per-breakpoint behaviour so the result works on a phone and on a 32-inch monitor,
4. validate the configuration,
5. publish it, and
6. export it as a bundle that can be reviewed, version-controlled, and imported elsewhere.

No deployment. No restart. No code.

---

## 2. The configuration application

`config-service` is a standalone Spring Boot application with its own database and its own OIDC
client.

| Property | Value |
| --- | --- |
| Runs independently | Yes — starts and is fully usable with every other service down |
| Audience | Configuration admins and business SMEs only |
| Authorisation | Realm role `mpria-config-admin`; publish additionally requires `mpria-config-publisher` |
| Storage | Own PostgreSQL schema. No other service writes to it |
| Interface | REST + OpenAPI, and the **admin console** (React SPA, separate bundle from the operator client) |
| Availability requirement | Low. It may be down during business hours without affecting cataloguing |

That last row is the design payoff: because consumers **pin to a published revision** and cache it,
`config-service` is not on the runtime critical path. It can be upgraded, restarted, or firewalled
to an internal network without touching availability of the operator application.

### 2.1 Revision lifecycle

```
        ┌──────────┐  validate   ┌───────────┐   publish   ┌───────────┐
        │  DRAFT   │────────────▶│ VALIDATED │────────────▶│ PUBLISHED │
        └──────────┘             └───────────┘             └───────────┘
             │                         │                         │
             │ edit (mutable)          │ immutable               │ immutable, event emitted
             ▼                         ▼                         ▼
      any number of drafts      one candidate            exactly one current per tenant
```

* A **draft** is the only mutable configuration. Nothing at runtime can see it.
* **Validation** is a server-side gate, not a UI convenience: referential integrity (every field
  referenced by a layout exists), schema compilability, expression compilation, no orphan slots,
  no duplicate field IDs, and a *migration check* against existing data.
* **Publishing** writes an immutable revision and emits `ConfigurationPublished(tenantId, revisionId)`.
* **Rollback is publishing an earlier revision.** Because revisions are immutable and content-
  addressed, rollback is a pointer move, not a restore.

### 2.2 Preview before publish

A draft can be previewed by requesting a presentation model with an explicit
`X-Config-Revision: draft:<id>` header. The BFF honours it only for principals holding
`mpria-config-admin`. The SME sees the real renderer with real data before anyone else is affected.

---

## 3. The layout model

### 3.1 Design constraints

* **No raster / absolute positioning.** v8's raster layout is why the UI cannot be responsive.
* **Hybrid flex + CSS Grid** (requirement #10): Grid for the two-dimensional form canvas, flex for
  one-dimensional internals.
* The layout must be **declarative data**, renderable by any client, with no embedded code.

### 3.2 Structure

A layout is a tree of four node kinds, modelled as a sealed hierarchy server-side:

```
Screen
└── Region        — a grid container; defines columns per breakpoint
    ├── Group     — a labelled, optionally collapsible sub-grid
    │   └── Slot  — occupies grid cells; holds exactly one component
    └── Slot
```

```jsonc
{
  "screenId": "object-detail",
  "recordType": "object",
  "regions": [
    {
      "id": "main",
      "kind": "REGION",
      "grid": { "columns": { "base": 1, "md": 6, "xl": 12 }, "gap": "md" },
      "children": [
        {
          "id": "identification",
          "kind": "GROUP",
          "label": "config.group.identification",
          "collapsible": true,
          "span":  { "base": 1, "md": 6, "xl": 8 },
          "grid":  { "columns": { "base": 1, "md": 2, "xl": 2 } },
          "children": [
            { "id": "s1", "kind": "SLOT", "span": { "base": 1, "md": 1 },
              "component": { "type": "TEXT_FIELD", "fieldId": "inventoryNumber" } },
            { "id": "s2", "kind": "SLOT", "span": { "base": 1, "md": 1 },
              "component": { "type": "TEXT_FIELD", "fieldId": "title" } },
            { "id": "s3", "kind": "SLOT", "span": { "base": 1, "md": 2 },
              "component": { "type": "RICH_TEXT", "fieldId": "description", "rows": 6 } }
          ]
        },
        {
          "id": "media",
          "kind": "SLOT",
          "span": { "base": 1, "md": 6, "xl": 4 },
          "component": { "type": "MEDIA_GALLERY", "relationType": "hasMedia" }
        }
      ]
    }
  ]
}
```

Notes on the shape:

* **`span` is per breakpoint, and breakpoints are named, not pixel values.** `base / sm / md / lg / xl`
  are defined once in the design tokens. An admin never types a pixel width.
* **`base` is single-column by default.** Mobile correctness is the default state, not an
  afterthought — an admin has to actively choose a multi-column mobile layout.
* A slot references a `fieldId`; it does **not** carry the field's label, type constraints, or
  validation. Those live in the field definition. Move a field on the canvas, and nothing about
  its semantics changes.
* No node carries a colour, font, or pixel dimension. The admin configures **structure**; the
  design system owns **appearance**. This is the boundary that keeps configured screens from
  degenerating into 200 mutually inconsistent designs.

### 3.3 Component registry

Each `component.type` is a contract between config-service (which offers it in the palette) and
the renderer (which draws it). The registry is served by `config-service` at
`GET /api/v1/components`, so the palette is never hardcoded in the admin console:

```jsonc
{
  "type": "TEXT_FIELD",
  "label": "Text field",
  "acceptsFieldTypes": ["STRING"],
  "minSpan": { "base": 1 },
  "options": {
    "multiline": { "type": "boolean", "default": false },
    "maxLength": { "type": "integer" }
  }
}
```

`acceptsFieldTypes` is what makes drag-and-drop safe: the designer refuses to drop a `DATE_PICKER`
onto a `STRING` field before the admin can create an invalid configuration.

---

## 4. The drag-and-drop designer

### 4.1 Layout of the tool itself

```
┌──────────────┬──────────────────────────────────────┬────────────────┐
│ PALETTE      │  CANVAS  (live grid, breakpoint tabs)│  INSPECTOR     │
│              │  ┌──────────────────────────────┐    │                │
│ Unplaced     │  │ base │ sm │ md │ lg │ xl │    │    │ Selected slot: │
│ fields       │  ├──────────────────────────────┤    │  · field       │
│  · owner     │  │ ┌────────┬────────┐ ┌──────┐ │    │  · component   │
│  · acquired  │  │ │ invNo  │ title  │ │      │ │    │  · span/bp     │
│              │  │ ├────────┴────────┤ │ media│ │    │  · visibility  │
│ Components   │  │ │ description     │ │      │ │    │    expression  │
│  · Text      │  │ └─────────────────┘ └──────┘ │    │                │
│  · Rich text │  └──────────────────────────────┘    │  Validation ✓  │
│  · Gallery   │                                      │                │
└──────────────┴──────────────────────────────────────┴────────────────┘
```

* **Palette** lists unplaced fields for the record type plus the component registry. A field
  already on the canvas is greyed — the tool prevents accidental duplicate placement.
* **Canvas** renders the *real* renderer against sample data, inside a resizable frame. Selecting
  a breakpoint tab resizes the frame; the admin sees the actual reflow rather than a schematic.
* **Inspector** edits the selected node: span per breakpoint, component options, a visibility
  expression, and read-only conditions.

### 4.2 Interaction rules

| Gesture | Result |
| --- | --- |
| Drag field from palette → grid cell | Creates a slot at that cell with the default component for the field type |
| Drag slot → another cell | Moves it; siblings reflow. Never overlaps — the grid has no z-axis |
| Drag slot edge | Changes `span` **for the currently selected breakpoint only** |
| Drop a group into a region | Creates a nested grid |
| Drop onto an incompatible target | Rejected with an inline reason (`acceptsFieldTypes` mismatch) |
| Keyboard: arrows / `[` `]` | Move and resize the selected slot. **The designer is fully keyboard-operable** — required for accessibility and much faster for bulk work |

### 4.3 Breakpoint inheritance

Spans cascade upward from `base`. Setting only `base` gives a layout that works everywhere. An
admin overrides a larger breakpoint only when they want a different arrangement there, and the
inspector shows inherited values in grey with a "reset to inherited" affordance. This keeps a
typical configuration to one or two breakpoint overrides rather than five full layouts.

### 4.4 Implementation notes

* Drag-and-drop uses **`@dnd-kit`** with a custom grid-cell collision detector — pointer, touch,
  and keyboard sensors out of the box, which is why it is preferred over HTML5 native DnD.
* Designer state is a **reducer over immutable layout JSON**, giving undo/redo for free and making
  the whole editor unit-testable without a DOM: dispatch actions, assert on the resulting JSON.
* Autosave writes the draft every few seconds with an `If-Match` ETag; a concurrent editor gets a
  conflict dialog rather than a silent overwrite.

---

## 5. Standard configuration plus customisation

Requirement #5 asks for "standard configuration + customised". Modelled as **base plus overlay**:

```
  mpria-base-config@3.2.0            (ships with the product, immutable, signed)
            │
            ├── overlay: customer-louvre@17    (only the differences)
            │
            ▼
     effective configuration          (deterministic merge, computed at publish)
```

An overlay is a list of typed operations, not a copy:

```jsonc
{
  "basedOn": "mpria-base-config@3.2.0",
  "operations": [
    { "op": "add",     "path": "/recordTypes/object/fields/creditLine",
      "value": { "type": "STRING", "maxLength": 500 } },
    { "op": "relabel", "path": "/recordTypes/object/fields/title",
      "locale": "fr", "value": "Titre de l'œuvre" },
    { "op": "move",    "path": "/layouts/object-detail/slots/s3",
      "to": { "region": "main", "group": "identification", "span": { "md": 2 } } },
    { "op": "hide",    "path": "/layouts/object-detail/slots/s9" }
  ]
}
```

Why operations rather than a forked copy:

* **Base upgrades survive.** When `mpria-base-config@3.3.0` ships, the overlay re-applies. Only
  operations whose target path disappeared conflict, and those are reported as a precise list
  rather than "your configuration differs".
* **Reviewable.** An overlay is a readable diff. A support engineer can answer "what did this
  customer change" in seconds.
* **Deletion is `hide`, not `remove`.** Base fields are never deleted by an overlay, so base data
  is never orphaned.

### 5.1 Export / import bundle

```
config-bundle-louvre-r17.zip
├── manifest.json          # tenant, revision, basedOn, createdAt, createdBy, checksum, signature
├── record-types/*.json
├── layouts/*.json
├── vocabularies/*.json
├── validations/*.json
└── i18n/{locale}.json
```

* The manifest carries a **content checksum and a signature**; import verifies both. An unsigned
  or altered bundle is rejected — this is a supply-chain boundary, since a configuration bundle
  can change what an application does.
* Import is **transactional and dry-runnable**: `POST /api/v1/configurations/import?dryRun=true`
  returns the validation report and the effective diff without writing anything.
* Bundles are plain JSON in a zip precisely so they can live in Git. Configuration review becomes
  a merge request.

---

## 6. From configuration to a rendered screen

```
 admin publishes revision R
        │
        │  ConfigurationPublished(tenant, R)
        ▼
 ┌──────────────┐        ┌──────────────────┐        ┌──────────────────┐
 │ catalog-svc  │◀──────▶│ presentation-bff │◀──────▶│   config-service │
 │ (data +      │  data  │  assembles PM    │ layout │  (revision R,    │
 │  payload     │        │                  │ schema │   cached forever)│
 │  validation) │        └────────┬─────────┘        └──────────────────┘
 └──────────────┘                 │ PresentationModel JSON
                                  ▼
                        ┌──────────────────┐
                        │ generic renderer │  maps component.type → React component
                        └──────────────────┘
```

Request path for `GET /api/v1/screens/object-detail/{recordId}`:

1. BFF resolves the current revision for the tenant (cached; immutable so cacheable indefinitely).
2. Fetches the layout and the field definitions for the record type at that revision.
3. Fetches the record from `catalog-service` and the effective permissions from `access-service`.
4. Evaluates visibility/read-only expressions against the record and principal.
5. Emits the presentation model: `layout` from config, `fields` from data + definitions + state,
   `actions` from lifecycle + permissions.

The renderer receives a tree it can draw without knowing that "object" or "inventory number"
exist. **That is the acceptance test for the whole design**, and it is what the walking skeleton
in `services/` and `web/` demonstrates.

---

## 7. Testing this layer

| What | How |
| --- | --- |
| Layout reducer (move, resize, undo) | Pure unit tests over JSON. No DOM |
| Drag-and-drop gestures | Testing Library with `@dnd-kit` keyboard sensor — deterministic, no flake |
| Validation gate | Property tests: a randomly mutated layout must either validate or produce a specific, actionable error — never a stack trace |
| Overlay merge | Golden tests: `base + overlay = expected effective config`, plus a base-upgrade conflict suite |
| Bundle round-trip | `export → import(dryRun) → import → export` must be byte-identical |
| Renderer generality | The renderer test suite is forbidden from referencing any domain field name. If a test says `"inventoryNumber"`, the renderer has leaked domain knowledge |

That last rule is worth enforcing literally — a lint rule over the renderer's test files. It is the
cheapest possible guard against the framework-independence goal quietly eroding.
