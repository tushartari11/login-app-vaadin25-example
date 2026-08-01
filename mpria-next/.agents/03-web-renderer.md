# Agent 3 — the generic web renderer

Read `.agents/SHARED-BRIEF.md` first for the project context and conventions. The Java/Maven
specifics there do not apply to you; the conventions and the "read before writing" list do.

**Your directory: `web/` only.** `services/config-service/` and `services/presentation-bff/` belong
to other agents running right now. Do not touch anything under `services/` or the root `pom.xml`.
`web/package.json` currently exists but is essentially empty — scaffold over it.

Read `docs/CONTRACTS.md` ("PresentationModel JSON" and the notes at its end — those notes are
rules), `ARCHITECTURE.md` §7 and §12, `docs/ui-configuration.md` §3 and §7, and
`services/pm-contract/src/main/java/com/zetcom/mpria/pm/Span.java` (the breakpoint cascade you must
reimplement exactly) and `LayoutNode.java`.

Environment: Node 24, npm, Docker available.

## What this is

**Next.js 15 (App Router) + React 19 + TypeScript strict.** A renderer that draws *any*
PresentationModel with zero domain knowledge. Adding a field type means registering a component;
adding a screen requires no frontend work at all. That property is the acceptance test for the
entire architecture.

## Build

1. **`render(pm: PresentationModel): ReactNode`** is the whole public surface. Dispatch
   `component.type` through a registry (`Record<string, FieldComponent>`). Implement at least:
   `TEXT_FIELD`, `RICH_TEXT` (textarea), `NUMBER_FIELD`, `DATE_FIELD`, `CHECKBOX`, `SELECT` (uses
   `FieldState.options`), and `MEDIA_GALLERY` as an unbound placeholder.
   An unknown type must render a visible, non-crashing fallback naming the missing type — a
   configuration can always outrun a client, and that must degrade, not explode.

2. **Types** in `src/lib/pm/types.ts`, matching `docs/CONTRACTS.md` exactly, with `LayoutNode` as a
   proper discriminated union on `kind`. Add a comment that Phase 1 generates these from the
   backend JSON Schema (ARCHITECTURE.md §12).

3. **Layout — the requirement that matters most. No raster layout; hybrid flex + CSS Grid.**
   - `REGION` and `GROUP` are CSS Grid containers; `grid.columns` →
     `grid-template-columns: repeat(N, minmax(0, 1fr))`.
   - A `SLOT`'s `span` → `grid-column: span N`.
   - Breakpoints `base sm md lg xl` are **named and cascade upward**: a value at `md` applies at
     `lg` and `xl` unless overridden. Implement exactly as `Span.resolve` does in Java, and
     unit-test it against the same cases as `SpanTest`.
   - Emit per-breakpoint CSS via custom properties and a small number of media queries. **Do not**
     compute layout in JS from window size — it must be correct in SSR before hydration.
   - **Container queries** for component internals (label/control/help stacking), per
     ARCHITECTURE.md §12: a field group in a narrow side panel must render differently from one in
     a wide main region *at the same viewport width*.
   - Flex only for one-dimensional internals: toolbars, action bar, label/control stacks.
   - Design tokens as CSS custom properties (spacing, colour, radii, type scale), light and dark
     via `prefers-color-scheme`.

4. **Field state applied mechanically** — `readOnly`, `required`, `hidden`, `errors` — with correct
   a11y: real `<label for>`, `aria-invalid`, `aria-describedby`, `aria-required`. The renderer must
   not know *why* a field is read-only.

5. **Actions render from `actions[]` only.** Never construct a URL. A disabled action surfaces its
   `disabledReason`; `confirm: true` requires confirmation before firing.

6. **Wiring** — a route that loads `/api/v1/screens/object-detail/{id}` from
   `${NEXT_PUBLIC_BFF_URL:-http://localhost:8080}`, **plus** a static fixture page `/demo` that
   renders a checked-in sample PM JSON with no backend running. `/demo` is what makes the skeleton
   demonstrable — make sure it works completely standalone.
   Auth: token handling goes in a route handler / server component, never `localStorage`
   (ARCHITECTURE.md §10). For the skeleton an env var or dev-only cookie is acceptable **if** the
   code and a comment make the Phase-1 Authorization-Code + PKCE target explicit.

## Tests — Vitest + @testing-library/react, a hard gate

- the breakpoint cascade (same cases as the Java `SpanTest`)
- a nested layout renders the right grid structure and spans
- `readOnly` / `required` / `hidden` / `errors` each produce the right DOM and ARIA
- an unknown `component.type` renders the fallback instead of throwing
- a disabled action is not clickable and surfaces its reason
- **CRITICAL, from `docs/ui-configuration.md` §7: no test file under `web/` may reference a domain
  field name.** No `"inventoryNumber"`, no `"title"`, no `"object"`. Use neutral ids (`fieldA`,
  `typeX`, `screen-1`). Write an actual test that scans the test sources and fails if such a name
  appears — that guard is itself a deliverable, because it is what stops the renderer quietly
  acquiring domain knowledge.

## Quality bar

TypeScript `strict`, no `any` (use `unknown` + narrowing), no unused exports, no TODOs, accessible
by construction. Comments explain *why*. British-English spelling in prose.

Add `web/README.md`: how to run dev and tests, how to point at the BFF, and what is skeleton scope
versus Phase 1.

## Done

`npm run test` passes **and** `npm run build` succeeds, both run from `web/` and observed by you.
Report the real commands and results — do not claim success you have not seen.
