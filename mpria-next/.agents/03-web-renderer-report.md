# Agent 3 — the generic web renderer · completion report

> Assignment: [`03-web-renderer.md`](03-web-renderer.md) · Directory: `web/`
> Date: 2026-08-01

**Status: done.** `npm run test` and `npm run build` both pass from `web/`, observed on a cleared
`.next`.

---

## 1. What was built

Against the six numbered items in the assignment:

**1 — `render(pm): ReactNode`.** `web/src/renderer/render.tsx` is the entire public surface.
`component.type` dispatches through `Record<string, FieldComponent>` (`registry.ts`). Seven types
registered: `TEXT_FIELD`, `RICH_TEXT` (textarea, `rows` from component options), `NUMBER_FIELD`,
`DATE_FIELD`, `CHECKBOX`, `SELECT` (from `FieldState.options`, never a compiled-in list) and
`MEDIA_GALLERY` as an unbound placeholder. An unregistered type renders a visible fallback naming
it; the rest of the screen keeps working. A registry passed via `RenderOptions` extends the
built-in set, which is the tested extension path.

**2 — Types.** `src/lib/pm/types.ts`, matching `docs/CONTRACTS.md`, with `LayoutNode` a
discriminated union on `kind`. Optionality follows `@JsonInclude(NON_NULL)`. The header comment
states that Phase 1 generates this file from the backend JSON Schema per ARCHITECTURE.md §12.
Because an `as PresentationModel` cast would be a lie the compiler cannot catch, `parse.ts` narrows
`unknown` → `PresentationModel` at runtime with readable errors; it deliberately accepts unknown
*component types* (the fallback handles those) but rejects unknown node *kinds*, since a renderer
cannot guess whether an unknown node is a container or a leaf.

**3 — Layout.** Hybrid flex + CSS Grid, no raster. `REGION`/`GROUP` are grid containers
(`repeat(N, minmax(0, 1fr))`); a `SLOT`'s span is `grid-column: span N`. The cascade in `span.ts`
mirrors `Span.resolve` exactly, including its rejection of unknown breakpoint names and spans below
1. It is resolved **at render time** into one custom property per breakpoint, and four static media
queries at 40/48/64/80rem select the applicable one — so the layout is complete in server HTML
before hydration. Container queries (`@container mp-cell`) drive label/control stacking, so an
identical field renders differently in a narrow panel and a wide region at the same viewport width.
Design tokens are CSS custom properties in `globals.css`, light and dark via
`prefers-color-scheme`.

**4 — Field state.** Applied mechanically, never interpreted. `FieldFrame` owns all accessibility
wiring in one place — real `<label for>`, `aria-required`, `aria-invalid`, `aria-readonly`,
`aria-describedby` → the message list — so a component cannot get it wrong because it never writes
it. `hidden` removes the grid cell rather than concealing it, so the grid closes up instead of
leaving a hole nobody configured.

**5 — Actions.** Rendered from `actions[]` only; no URL constructed anywhere in the renderer. A
disabled action uses `aria-disabled` rather than `disabled` — `disabled` leaves the tab order, so a
keyboard user could never reach the `disabledReason`, which is rendered and linked by
`aria-describedby`. `confirm: true` gets an inline two-step confirmation, not `window.confirm`.

**6 — Wiring.** `/screens/[screenId]/[recordId]` is generic (`object-detail` is one value of it)
and fetches from `${NEXT_PUBLIC_BFF_URL:-http://localhost:8080}` in a server component. `/demo`
renders a checked-in model with nothing else running. Tokens are read only in
`lib/auth/access-token.ts` — env var or dev-only cookie, never `localStorage` — with the Phase 1
Authorization Code + PKCE target spelled out in both the code and the README.

---

## 2. Tests — real counts

**101 tests across 11 files**, all passing.

| File | Tests | Covers |
| --- | ---: | --- |
| `lib/pm/__tests__/span.test.ts` | 14 | the cascade, same table as the Java `SpanTest` |
| `lib/pm/__tests__/parse.test.ts` | 6 | wire narrowing, NON_NULL defaults, rejected shapes |
| `renderer/__tests__/layout.test.tsx` | 7 | nested grid structure, spans, gap tokens, collapsible groups |
| `renderer/__tests__/field-state.test.tsx` | 11 | `readOnly`/`required`/`hidden`/`errors` + ARIA |
| `renderer/__tests__/components.test.tsx` | 9 | each built-in control |
| `renderer/__tests__/unknown-component.test.tsx` | 4 | the fallback; registering a new type |
| `renderer/__tests__/actions.test.tsx` | 10 | disabled reasons, confirmation, values round-trip |
| `renderer/__tests__/ssr.test.tsx` | 3 | the cascade is in server HTML; no layout script |
| `app/demo/__tests__/demo-model.test.tsx` | 3 | the checked-in demo model still parses and renders |
| `renderer/__tests__/domain-vocabulary.guard.test.ts` | 12 | **the guard** |
| `renderer/__tests__/responsive-strategy.guard.test.ts` | 22 | no JS layout; the stylesheet's contract |

**The domain-vocabulary guard** (`docs/ui-configuration.md` §7, made executable) scans every test
source under `src/` and fails on a whole-word, case-insensitive match against the customer
vocabulary in the project's own examples. It excludes only itself, and asserts it found files to
scan — a guard that silently scans nothing is worse than no guard. It caught two genuine leaks
while it was being written (`"not an object at all"` and `"a URL object"`), which is the evidence
that it works rather than merely exists.

The second guard is not in the assignment but earns its place: it fails the build if any renderer
source touches `innerWidth`, `outerWidth`, `matchMedia`, `ResizeObserver`, `getBoundingClientRect`
or `useLayoutEffect`, and asserts the stylesheet's contract (exactly four viewport media queries, a
container query, `repeat(…, minmax(0, 1fr))`, no `position: absolute`). "Do not compute layout in
JS from window size" is a rule that erodes under one plausible-looking commit; this makes that
commit fail.

---

## 3. Verification actually performed

```
$ npm run test                    →  11 files, 101 tests, all passing
$ rm -rf .next && npm run build   →  compiled successfully, 6/6 static pages, no warnings
$ npx tsc --noEmit                →  exit 0
```

Beyond the two required gates, the things a green build does not prove were checked separately:

- **`next start` smoke test.** `/demo` → 200 with no backend running; `/` → 200;
  `/screens/screen-1/rec-1` → 200 showing the readable "BFF unreachable" panel rather than a stack
  trace.
- **The prerendered `/demo` HTML** carries `--mp-cols-xl:12`, `--mp-span-xl:8`,
  `aria-invalid="true"`, `aria-readonly="true"`, `aria-required="true"`, the `<details>` disclosure
  and the disabled action's reason — and the hidden field's slot is absent from the DOM.
- **The compiled CSS bundle** retains `@container mp-cell (min-width: 30rem)`, all four media
  queries, and `grid-template-columns:repeat(var(--mp-cols-*),minmax(0,1fr))`. Source CSS passing a
  string match is not the same as CSS surviving the build.

---

## 4. Contract items that could not be honoured

**1. The BFF's write request body is unspecified.** `docs/CONTRACTS.md` fixes the paths and methods
for `PUT /api/v1/screens/{screenId}/{recordId}` and `POST /api/v1/screens/{screenId}`, and assembly
rule 6 requires `meta.etag` to be echoed as `If-Match` — but not what the body looks like.
`src/app/api/actions/route.ts` sends `{"values": {fieldId: value}}` and sets `If-Match`. **That
body shape is this client's guess and needs confirming with `presentation-bff`.** It is the one
place the client and the BFF can silently disagree, because both sides will look correct in
isolation.

**2. `hidden: true` does not make a value secret, and the contract implies it might.** Verified
concretely: the demo fixture's hidden field renders no cell, but its value
`"Not visible to this principal."` is still present in the prerendered payload, because the whole
`PresentationModel` is serialised to the client. The renderer cannot fix this without making an
authorisation decision ARCHITECTURE.md §7.1 explicitly forbids it from making. **If a field is
hidden because the principal may not see it, the BFF must omit or null the value, not only set the
flag.** Worth an explicit line in `CONTRACTS.md`.

Everything else in the contract was honoured as written. No renames were needed, so nothing
`catalog-service` consumes was disturbed.

---

## 5. Worth propagating to the other two agents

- **`@JsonInclude(NON_NULL)` drops `null` but not `[]`.** `errors`, `options` and `capabilities`
  arrive as empty arrays, not as absent keys. The client parser tolerates both; if either service
  relies on absence meaning empty, check which it actually emits.
- **`ActionModel`'s "a disabled action must state a reason" invariant is re-enforced client-side.**
  If the BFF ever emits `enabled: false` with no `disabledReason`, this client rejects the whole
  model rather than rendering an inert unexplained button. That is deliberate, but it means a lax
  BFF turns a cosmetic defect into a blank screen.
- **The cascade is now implemented twice** — `Span.java` and `span.ts` — and tested against the same
  table on both sides. Any change to one is a change to two.
- No Boot 4 surprises reached this assignment. The frontend equivalents were Vitest-specific: under
  the jsdom environment the global `URL` is jsdom's, which `fileURLToPath` refuses, so any test
  resolving its own path must pass a string.

---

## 6. Scope deliberately not built

No i18n bundle (labels render verbatim, so the demo fixture carries human text where a real model
carries `config.field.…` keys); `MEDIA_GALLERY` is a placeholder pending Phase 5; no Playwright,
which ARCHITECTURE.md §14 puts at the tip and not in Phase 0; auth is the env-var/cookie stand-in
the assignment permits.

**There is also no ESLint in this project.** The static checks are TypeScript `strict` (with
`noUncheckedIndexedAccess` and `noUnusedLocals`) plus the two guard tests. Worth knowing, since "no
compiler warnings" and "no lint warnings" are not the same claim, and only the first can be made
here.

---

## 7. Files touched outside `web/`

One, and only on the maintainer's explicit later instruction: `README.md` §4 now says how to pass
the dev token to the web client (`MPRIA_DEV_ACCESS_TOKEN=… npm run dev`, plus the note that `/demo`
needs neither token nor backend). The shared brief forbade edits outside `web/`, so it is called
out here rather than buried. It is a shared file and not under git in this checkout, so another
agent rewriting that section would take the paragraph with it.
