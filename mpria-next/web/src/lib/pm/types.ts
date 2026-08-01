/**
 * The PresentationModel wire contract, as documented in `docs/CONTRACTS.md` and produced by
 * `services/pm-contract` (`com.zetcom.mpria.pm.*`).
 *
 * These declarations are hand-written **for the walking skeleton only**. Per ARCHITECTURE.md §12
 * the Phase 1 target is to generate this file from the backend's JSON Schema with
 * `json-schema-to-typescript` and to fail the *backend* build on drift, so a server-side change
 * that breaks the client cannot merge. Until that generator exists, `parsePresentationModel` in
 * `./parse.ts` is the runtime check that keeps this file honest.
 *
 * Optionality here follows the server's `@JsonInclude(NON_NULL)`: absent keys are omitted, never
 * null. Java primitives (`boolean`) and normalised collections are therefore always present, while
 * genuinely nullable references are optional. `grid` and `span` are additionally optional because
 * a hand-authored layout fixture may omit them; the renderer defaults both to a single column
 * rather than failing, which matches `Span.normalise`.
 */

/** Named breakpoints. Never pixel values — those live once, in the design tokens. */
export type Breakpoint = 'base' | 'sm' | 'md' | 'lg' | 'xl';

/**
 * Column counts or spans per named breakpoint, cascading upward: a value at `md` also applies at
 * `lg` and `xl` unless overridden.
 */
export type SpanMap = Partial<Record<Breakpoint, number>>;

export interface GridSpec {
  readonly columns: SpanMap;
  /** A spacing token name from the design system (`xs`…`xl`), never a length. */
  readonly gap?: string;
}

export interface ComponentRef {
  /** Registry key, e.g. `TEXT_FIELD`. Unknown keys must render a fallback, never throw. */
  readonly type: string;
  /** Absent for data-less components such as `MEDIA_GALLERY`. */
  readonly fieldId?: string;
  readonly options?: Readonly<Record<string, unknown>>;
}

export interface RegionNode {
  readonly kind: 'REGION';
  readonly id: string;
  readonly grid?: GridSpec;
  readonly children: readonly LayoutNode[];
}

export interface GroupNode {
  readonly kind: 'GROUP';
  readonly id: string;
  /** An i18n key, never a literal caption. */
  readonly label?: string;
  readonly collapsible?: boolean;
  readonly span?: SpanMap;
  readonly grid?: GridSpec;
  readonly children: readonly LayoutNode[];
}

export interface SlotNode {
  readonly kind: 'SLOT';
  readonly id: string;
  readonly span?: SpanMap;
  readonly component: ComponentRef;
}

/**
 * Discriminated on `kind`, mirroring the sealed `LayoutNode` hierarchy in Java. Exhaustive
 * `switch` over this union is what gives the renderer the same compile-time guarantee the server
 * has: adding a node kind cannot silently skip a branch.
 */
export type LayoutNode = RegionNode | GroupNode | SlotNode;

export interface Layout {
  readonly screenId: string;
  readonly recordType: string;
  readonly regions: readonly LayoutNode[];
}

export interface FieldOption {
  readonly value: string;
  readonly label: string;
}

export interface FieldState {
  readonly type: string;
  readonly label?: string;
  readonly value?: unknown;
  readonly options: readonly FieldOption[];
  readonly readOnly: boolean;
  readonly required: boolean;
  readonly hidden: boolean;
  readonly errors: readonly string[];
}

export interface ActionModel {
  readonly id: string;
  readonly label: string;
  readonly method: string;
  /** Server-supplied. The client never builds this — see `docs/CONTRACTS.md`. */
  readonly href: string;
  readonly enabled: boolean;
  readonly disabledReason?: string;
  readonly confirm: boolean;
}

export interface ScreenMeta {
  readonly screenId: string;
  readonly revisionId: string;
  readonly recordId?: string;
  readonly etag?: string;
  readonly capabilities: readonly string[];
}

export interface PresentationModel {
  readonly meta: ScreenMeta;
  readonly layout: Layout;
  /** Keyed by field id. Every field the layout binds is guaranteed present by the server. */
  readonly fields: Readonly<Record<string, FieldState>>;
  readonly actions: readonly ActionModel[];
}
