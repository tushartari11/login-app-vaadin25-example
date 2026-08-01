# `web/` — the generic renderer

The operator client. It asks the presentation BFF for **a screen**, never for a record of a
particular kind, and draws whatever tree it is handed.

There is no field name, no record type and no business rule in any of this client's code — the one
file under `src/` that names customer fields is `src/app/demo/demo-presentation-model.json`, which
is configuration data rather than source. Adding a field type is registering a component in
`src/renderer/registry.ts`; **adding a screen requires no frontend work at all**. That asymmetry is
the acceptance test for the whole server-driven design
([`ARCHITECTURE.md` §7.3](../ARCHITECTURE.md)), and a test fails the build if the renderer's own
test suite starts speaking the customer's vocabulary.

Next.js 15 (App Router) · React 19 · TypeScript `strict` · Vitest + Testing Library.

---

## Running it

```bash
cd web
npm install

npm run dev        # http://localhost:3000
npm run test       # Vitest, no backend needed
npm run build      # production build; also type-checks
npm run typecheck  # types only
npm run lint:domain  # just the domain-vocabulary guard
```

### `/demo` — the renderer with nothing else running

`http://localhost:3000/demo` renders a checked-in presentation model
(`src/app/demo/demo-presentation-model.json`). No BFF, no database, no Keycloak. It deliberately
exercises the awkward cases: a read-only field, a field carrying a server-side validation message,
a field the model marks `hidden`, an action disabled with a stated reason, an action that requires
confirmation, and a component type this client does not implement.

Resize the window to watch the configured per-breakpoint spans take effect. Nothing reflows on
hydration, because nothing was computed in JavaScript.

### A live screen

```
/screens/<screenId>/<recordId>   →   GET ${NEXT_PUBLIC_BFF_URL}/api/v1/screens/<screenId>/<recordId>
```

The route is generic; `/screens/object-detail/<id>` is just one value of it.

```bash
NEXT_PUBLIC_BFF_URL=http://localhost:8080 \
MPRIA_DEV_ACCESS_TOKEN="$(…)" \
npm run dev
```

`NEXT_PUBLIC_BFF_URL` defaults to `http://localhost:8080`. The BFF is the only service this client
talks to.

### Authentication in the skeleton

Tokens are held **server-side only** — never `localStorage` ([`ARCHITECTURE.md` §10](../ARCHITECTURE.md)).
`src/lib/auth/access-token.ts` is the single place that reads one, from either the
`MPRIA_DEV_ACCESS_TOKEN` environment variable or a dev-only `mpria_dev_access_token` cookie you set
by hand. Both are stand-ins.

**Phase 1** replaces them with Authorization Code + PKCE against Keycloak: a callback route handler
exchanges the code, keeps the refresh token in an encrypted `HttpOnly` `SameSite=Lax` cookie, and
attaches a short-lived access token server-side. The browser never sees either. What does *not*
change is where a token is allowed to go, which is why every consumer today is already a server
component or a route handler.

---

## How it is put together

```
src/
├── lib/pm/
│   ├── types.ts      the wire contract; LayoutNode is a discriminated union on `kind`
│   ├── span.ts       the breakpoint cascade, mirroring Span.java
│   └── parse.ts      unknown → PresentationModel, with readable failures
├── renderer/
│   ├── render.tsx            render(pm) — the entire public surface
│   ├── registry.ts           component.type → component
│   ├── LayoutNodeView.tsx    the tree walk (exhaustive over the union)
│   ├── ActionBar.tsx         actions[], confirmation, disabled reasons
│   ├── grid.ts               the cascade → CSS custom properties
│   ├── renderer.module.css   the layout engine
│   └── components/           FieldFrame (a11y wiring) + the built-in controls
└── app/                      routes: /, /demo, /screens/[screenId]/[recordId], /api/actions
```

### Layout: CSS, never JavaScript

Grid owns the two-dimensional form canvas; flex owns one-dimensional internals (the action bar, the
label/control stack). Nothing is positioned absolutely — replacing v8's raster layout is the point.

* A `REGION` and a `GROUP` are grid containers. `grid.columns` becomes
  `grid-template-columns: repeat(N, minmax(0, 1fr))`.
* A `SLOT`'s `span` becomes `grid-column: span N`.
* Breakpoints `base sm md lg xl` are **names**, and they **cascade upward**: a value at `md` also
  applies at `lg` and `xl` unless overridden. Implemented in `span.ts` exactly as `Span.resolve`
  does in Java, and unit-tested against the same table as `SpanTest`.

The cascade is resolved **during render** into one custom property per breakpoint
(`--mp-span-md: 6`), and four static media queries at `40/48/64/80rem` pick the one that applies.
So the layout is already correct in server-rendered HTML, before hydration and without JavaScript
at all. A guard test fails the build if any renderer source measures anything — `innerWidth`,
`outerWidth`, `matchMedia`, `ResizeObserver`, `getBoundingClientRect` or `useLayoutEffect`.

**Container queries** handle component internals: a field stacks its label above its control in a
narrow cell and puts them side by side in a wide one — decided by the cell, so a field group in a
narrow side panel renders differently from an identical one in a wide main region *at the same
viewport width* ([`ARCHITECTURE.md` §12](../ARCHITECTURE.md)).

Design tokens (spacing, colour, radii, type scale) are CSS custom properties in
`src/app/globals.css`, with a dark scheme via `prefers-color-scheme`. Configuration carries
structure; the design system owns appearance, and no layout node may carry a colour, a font or a
pixel dimension.

### Field state, applied mechanically

`readOnly`, `required`, `hidden` and `errors` are applied without the renderer knowing *why* they
hold. `FieldFrame` owns all of the accessibility wiring in one place — a real `<label for>`,
`aria-required`, `aria-invalid`, `aria-readonly`, `aria-describedby` pointing at the messages — so
a component cannot get it wrong, because it never writes it.

`hidden` removes the whole grid cell rather than hiding it, so the surrounding grid closes up
instead of leaving a hole its author never configured.

### Actions

Rendered from `actions[]` and nothing else. No URL is ever constructed and no button's availability
is ever decided here. A disabled action uses `aria-disabled` rather than the `disabled` attribute —
`disabled` would remove it from the tab order, so a keyboard user could never reach the explanation
— and its `disabledReason` is rendered and linked with `aria-describedby`. `confirm: true` gets an
inline two-step confirmation, not `window.confirm`.

### Unknown component types

Configuration is published without deploying this client, so a configuration will outrun it sooner
or later. An unregistered `component.type` renders a visible fallback naming the type; the rest of
the screen keeps working. An unknown layout node `kind` is rejected at parse time instead, because
a renderer cannot guess whether an unknown node is a container or a leaf.

---

## Tests

```
npm run test     # 101 tests in 11 files
```

* `lib/pm/__tests__/span.test.ts` — the cascade, against the same cases as the Java `SpanTest`
* `lib/pm/__tests__/parse.test.ts` — wire payload narrowing and the invariants it enforces
* `renderer/__tests__/layout.test.tsx` — a nested layout's grid structure and spans
* `renderer/__tests__/field-state.test.tsx` — `readOnly` / `required` / `hidden` / `errors` and ARIA
* `renderer/__tests__/components.test.tsx` — each built-in control
* `renderer/__tests__/unknown-component.test.tsx` — the fallback, and registering a new type
* `renderer/__tests__/actions.test.tsx` — disabled reasons, confirmation, values round-trip
* `renderer/__tests__/ssr.test.tsx` — the layout is complete in server-rendered HTML
* `app/demo/__tests__/demo-model.test.tsx` — the checked-in demo model still parses and renders
* `renderer/__tests__/domain-vocabulary.guard.test.ts` — **the guard**
* `renderer/__tests__/responsive-strategy.guard.test.ts` — no JS layout; the stylesheet's contract

### The domain-vocabulary guard

From [`docs/ui-configuration.md` §7](../docs/ui-configuration.md): *"The renderer test suite is
forbidden from referencing any domain field name."*

That guard is executable. Every renderer test uses meaningless identifiers — `fieldA`, `typeX`,
`screen-1` — and `domain-vocabulary.guard.test.ts` scans every test source under `src/` and fails on
a whole-word, case-insensitive match against the customer vocabulary in the project's own examples.

It is enforced this way because the property erodes silently. Nobody sets out to hardcode a field
name; a test written against a realistic screen quietly bakes one in, the next test copies it, and
once the test suite knows the domain the renderer is free to know it too with nothing failing.

The one exception is `src/app/demo/demo-presentation-model.json`, which is **configuration data**,
not a test. Configuration is exactly where customer vocabulary belongs.

---

## Skeleton scope versus Phase 1

| | Now (Phase 0) | Phase 1 |
| --- | --- | --- |
| PM types | Hand-written in `lib/pm/types.ts`, guarded by a runtime parser | **Generated** from the backend JSON Schema; a backend change that breaks the client fails the *backend* build (§12) |
| Auth | Env var or dev cookie, server-side only | Authorization Code + PKCE, encrypted `HttpOnly` refresh cookie |
| i18n | Labels are rendered verbatim, so the demo fixture carries human text where a real model carries keys such as `config.field.…` | A message bundle per locale, resolved client-side |
| Components | Seven built-ins | The full field-type catalogue; the registry is served by `config-service` at `GET /api/v1/components` |
| `MEDIA_GALLERY` | Unbound placeholder | Backed by `media-service` (Phase 5) |
| Write path | `PUT`/`POST` via `/api/actions`, then `router.refresh()` | Optimistic update against the returned model |
| Admin console | Not here | The drag-and-drop layout designer — a separate bundle and OIDC client (§12) |
| E2E | None | Playwright, two or three journeys, nightly (§14) |

### Two contract notes for the backend services

1. **The BFF's write request body is unspecified.** `docs/CONTRACTS.md` fixes the paths and methods
   for `PUT /api/v1/screens/{screenId}/{recordId}` and `POST /api/v1/screens/{screenId}`, and rule 6
   of the assembly rules requires `meta.etag` to be echoed as `If-Match` — but not what the body
   looks like. `src/app/api/actions/route.ts` sends `{"values": {fieldId: value}}` and sets
   `If-Match`. That body shape is this client's guess and needs confirming.
2. **`hidden: true` does not make a value secret.** The renderer removes the cell, but the value is
   still in the model and therefore still reaches the browser. If a field is hidden because the
   principal may not see it, the BFF must omit or null the value, not only set the flag.
