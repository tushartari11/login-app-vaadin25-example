# Inter-service contracts (walking skeleton)

Frozen shapes for the Phase-0 vertical slice. **These are binding**: `catalog-service` already
consumes the schema endpoint, so changing a field name here breaks a service someone else is
building. Propose changes rather than making them.

Ports: `presentation-bff` 8080 · `catalog-service` 8081 · `config-service` 8082 ·
Keycloak 8180 · web 3000.

All calls carry `Authorization: Bearer <jwt>`. Tenant comes from the token's `tenant_id` claim,
never from a parameter. Internal service-to-service calls additionally send `X-Tenant-Id` when the
callee needs it before token parsing.

---

## config-service (8082)

> **Skeleton deviation, accepted.** `config-service` accepts `X-Tenant-Id` on every endpoint and
> **ignores it**: the in-memory store holds one configuration for the installation, which matches
> the on-premises MVP where a deployment serves a single customer. Callers must keep sending the
> header so nothing changes at the call site when Phase 2 adds PostgreSQL and partitions by tenant.
> Deliberately *not* pinned to the fixture tenant `1111…` — a hardcoded tenant that disagreed with
> a real token would 404 the whole demo while protecting nothing.

### `GET /api/v1/record-types/{recordType}/schema`

Header: `X-Tenant-Id: <uuid>`. **Already consumed by `catalog-service`** — field names are fixed.

```jsonc
{
  "recordType": "object",
  "schemaVersion": 1,
  "titleFieldId": "title",
  "fields": [
    { "id": "inventoryNumber", "type": "STRING", "label": "config.field.inventoryNumber",
      "required": true, "maxLength": 50, "allowedValues": [] },
    { "id": "condition", "type": "ENUM", "label": "config.field.condition",
      "required": false, "maxLength": null, "allowedValues": ["GOOD", "FAIR", "POOR"] }
  ]
}
```

`type` is one of `STRING | TEXT | INTEGER | DECIMAL | BOOLEAN | DATE | ENUM`
(mirrors `com.zetcom.mpria.catalog.domain.FieldType`).

404 when the record type is not defined in the current published revision.

### `GET /api/v1/screens/{screenId}`

Header: `X-Tenant-Id`. Returns the configured layout for a screen.

```jsonc
{
  "screenId": "object-detail",
  "recordType": "object",
  "revisionId": "rev-1",
  "layout": { /* pm-contract Layout JSON — see below */ }
}
```

`layout` is exactly the JSON produced by `com.zetcom.mpria.pm.Layout`. Do not invent a parallel
shape; depend on `pm-contract` and serialise the real type.

### `GET /api/v1/revisions/current`

```jsonc
{ "revisionId": "rev-1", "publishedAt": "2026-08-01T10:15:30Z", "status": "PUBLISHED" }
```

### `GET /api/v1/components`

The component registry that drives the admin palette and the client's component map.

```jsonc
[
  { "type": "TEXT_FIELD", "label": "Text field", "acceptsFieldTypes": ["STRING"],
    "options": { "multiline": { "type": "boolean", "default": false } } },
  { "type": "MEDIA_GALLERY", "label": "Media gallery", "acceptsFieldTypes": [], "options": {} }
]
```

### Admin endpoints (role `mpria-config-admin`)

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/admin/drafts/{draftId}` | Read the mutable draft |
| `PUT` | `/api/v1/admin/drafts/{draftId}/layouts/{screenId}` | Replace a screen layout (If-Match) |
| `POST` | `/api/v1/admin/drafts/{draftId}/publish` | Validate then publish; emits a new `revisionId`. **Additionally requires `mpria-config-publisher`** — a principal holding only one of the two roles gets 403 |
| `GET` | `/api/v1/admin/revisions/{revisionId}/export` | Bundle download (JSON) |
| `POST` | `/api/v1/admin/import?dryRun=true` | Validate a bundle, report the effective diff |

Publishing must reject a layout that binds a field the record type does not define. That
validation is the point of the gate.

---

## catalog-service (8081) — already built

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/api/v1/records` | `{recordType, identifier, data}` → 201 + `ETag` |
| `GET` | `/api/v1/records/{id}` | → 200 + `ETag` |
| `GET` | `/api/v1/records?recordType=&limit=` | list, newest first |
| `PUT` | `/api/v1/records/{id}` | `If-Match: "<version>"`, body `{identifier, data}` |
| `POST` | `/api/v1/records/{id}/lifecycle/{DRAFT\|PUBLISHED\|ARCHIVED}` | transition |

Record response:

```jsonc
{
  "id": "...", "recordType": "object", "schemaVersion": 1,
  "identifier": "INV-0001", "title": "Study of a hand", "lifecycle": "DRAFT",
  "data": { "inventoryNumber": "INV-0001", "title": "Study of a hand" },
  "etag": "\"0\"",
  "createdAt": "...", "createdBy": "...", "updatedAt": "...", "updatedBy": "..."
}
```

Errors are RFC 9457 problem documents. Validation failures return **422** with:

```jsonc
{ "type": "https://docs.mpria.zetcom.com/problems/invalid-record",
  "status": 422,
  "errors": { "inventoryNumber": ["is required"], "acquiredOn": ["must be an ISO-8601 date (yyyy-MM-dd)"] } }
```

Other statuses: 404 `record-not-found`, 400 `unknown-record-type`, 412 `stale-record`,
409 `concurrent-modification`, 403 `incomplete-token`.

---

## presentation-bff (8080)

The only service the web client talks to. Returns `com.zetcom.mpria.pm.PresentationModel` JSON.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/screens/{screenId}/new?recordType=` | blank form |
| `GET` | `/api/v1/screens/{screenId}/{recordId}` | populated form |
| `PUT` | `/api/v1/screens/{screenId}/{recordId}` | submit; returns the re-rendered PM |
| `POST` | `/api/v1/screens/{screenId}` | create; returns the re-rendered PM |
| `POST` | `/api/v1/screens/{screenId}/{recordId}/lifecycle/{DRAFT\|PUBLISHED\|ARCHIVED}` | transition; returns the re-rendered PM |

The lifecycle path is what the model's `publish` and `archive` actions carry in `actions[].href`, so
a client follows the URL it was given rather than composing one. No request body: the target state is
part of the addressed resource. An unknown state is `400` before any use case is entered.

### Write request body

Originally left unspecified here — an omission the client and the BFF could have silently
disagreed about. Fixed shape, for both `PUT` and `POST`:

```jsonc
{
  "identifier": "INV-0001",             // optional; the spine identifier
  "values": { "<fieldId>": <value> }    // only fields the layout binds
}
```

* `PUT` sends `If-Match: <meta.etag>` from the model it rendered. The BFF forwards it to
  catalog-service unchanged.
* The response is always a `PresentationModel` — on success, on validation failure (200 with
  `FieldState.errors` populated), and after a lifecycle change.
* `412` propagates unchanged when the client's copy was stale: that is a genuine protocol
  outcome, not a field error, and the client must reload rather than merge. catalog-service's `409`
  `concurrent-modification` is the same situation detected one layer deeper and is reported as `412`
  too — for the client both mean *re-read before writing again*.
* `409` from the BFF means only one thing: `illegal-transition`, the move is not permitted from the
  record's current state. Note that catalog-service answers **both** an illegal transition and a lost
  write race with `409`, so the BFF dispatches on the problem document's `type` rather than on the
  status — the two need different things from the user.

Assembly rules:

1. Resolve the current `revisionId` from config-service (cache by revision — immutable).
2. Fetch layout + schema for the screen's record type.
3. Fetch the record from catalog-service (skip for `new`).
4. Build `fields`: one `FieldState` per **field the layout binds**, merging the schema's
   `type`/`label`/`required`/`allowedValues` with the record's value.
5. Build `actions` from lifecycle: `save` always; `publish` enabled only in `DRAFT`; `archive`
   disabled with reason `record.alreadyArchived` when already `ARCHIVED`.
6. `meta.etag` mirrors the record's ETag and must be echoed as `If-Match` on the next write.

**A validation failure from catalog-service is not an error response.** Catch the 422, map
`errors[fieldId]` onto `FieldState.errors`, and return **200** with the re-rendered model. The
form must come back with messages attached to the right controls, not a stack trace.

`PresentationModel`'s constructor enforces that every layout-bound field has a `FieldState`. Let
it — do not work around it.

### `hidden` is a rendering hint, never a confidentiality mechanism

**The BFF must omit the value of any field it hides for authorisation reasons** — send
`"value": null`, not the real value with `hidden: true`.

This was found by rendering the model: the renderer correctly drops the grid cell, but the value
still travels to the browser and remains readable in the server-rendered payload. The client
cannot fix it, because deciding *whether a value may be seen* is exactly the authorisation
decision §7.1 of `ARCHITECTURE.md` says belongs on the server.

So the two cases are distinct and must be treated differently:

| Why the field is hidden | `hidden` | `value` |
| --- | --- | --- |
| Conditional visibility — irrelevant given other field values, but the principal may see it | `true` | may be sent |
| The principal is not permitted to see it | `true` | **must be `null`** |

When in doubt, null it. A field the user cannot see is a field the client has no use for.

---

## PresentationModel JSON (from `pm-contract`)

```jsonc
{
  "meta": { "screenId": "object-detail", "revisionId": "rev-1", "recordId": "...",
            "etag": "\"0\"", "capabilities": ["EDIT"] },
  "layout": {
    "screenId": "object-detail", "recordType": "object",
    "regions": [
      { "kind": "REGION", "id": "main",
        "grid": { "columns": { "base": 1, "md": 6, "xl": 12 }, "gap": "md" },
        "children": [
          { "kind": "GROUP", "id": "grp-identification", "label": "config.group.identification",
            "collapsible": true, "span": { "base": 1, "md": 6, "xl": 8 },
            "grid": { "columns": { "base": 1, "md": 2 }, "gap": "md" },
            "children": [
              { "kind": "SLOT", "id": "slot-inv", "span": { "base": 1 },
                "component": { "type": "TEXT_FIELD", "fieldId": "inventoryNumber", "options": {} } }
            ] }
        ] }
    ]
  },
  "fields": {
    "inventoryNumber": { "type": "STRING", "label": "config.field.inventoryNumber",
                         "value": "INV-0001", "options": [], "readOnly": false,
                         "required": true, "hidden": false, "errors": [] }
  },
  "actions": [
    { "id": "save", "label": "action.save", "method": "PUT",
      "href": "/api/v1/screens/object-detail/<id>", "enabled": true, "confirm": false },
    { "id": "archive", "label": "action.archive", "method": "POST", "href": "...",
      "enabled": false, "disabledReason": "record.alreadyArchived", "confirm": true }
  ]
}
```

Notes for client implementers:

* `kind` discriminates layout nodes — dispatch on it.
* `span` and `grid.columns` are **named breakpoints** (`base sm md lg xl`) that **cascade upward**:
  a value defined at `md` applies at `lg` and `xl` unless overridden. `base` always exists.
* Absent keys are omitted, not null (`@JsonInclude(NON_NULL)`).
* Never construct a URL. Use `actions[].href`.
* `hidden: true` is a layout instruction, not a redaction. The value still travels in the payload and
  is readable in the browser, so a renderer must not treat a hidden field as secret — and must not try
  to strip it either, because deciding whether a value may be seen is the server's call
  (`ARCHITECTURE.md` §7.1). Where the principal may not see a field, the BFF nulls `value` before it
  is serialised; see the `hidden` section above for the two cases.
