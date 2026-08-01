import { normaliseSpan } from '@/lib/pm/span';
import type {
  ActionModel,
  ComponentRef,
  FieldOption,
  FieldState,
  GridSpec,
  Layout,
  LayoutNode,
  PresentationModel,
  ScreenMeta,
  SpanMap,
} from '@/lib/pm/types';

/**
 * Narrows an untyped wire payload into a {@link PresentationModel}.
 *
 * <p>`fetch(...).json()` returns `unknown`, and a `as PresentationModel` cast would be a lie the
 * type system cannot catch. Parsing instead gives one place where a malformed response becomes a
 * readable error rather than a `undefined is not a function` three components deep.
 *
 * <p>The rule it deliberately does **not** enforce is the component registry. An unknown
 * `component.type` parses fine and renders a fallback: configuration is published independently of
 * the client, so a configuration will eventually outrun a deployed renderer, and that must degrade
 * rather than blank the screen.
 *
 * <p>Phase 1 replaces this with a validator generated from the backend JSON Schema
 * (ARCHITECTURE.md §12).
 */
export function parsePresentationModel(input: unknown): PresentationModel {
  const root = requireObject(input, 'PresentationModel');
  return {
    meta: parseMeta(root['meta']),
    layout: parseLayout(root['layout']),
    fields: parseFields(root['fields']),
    actions: parseArray(root['actions'], 'actions').map(parseAction),
  };
}

function requireObject(value: unknown, what: string): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(`${what} must be a JSON object but was ${describe(value)}`);
  }
  return value as Record<string, unknown>;
}

function describe(value: unknown): string {
  return value === null ? 'null' : Array.isArray(value) ? 'an array' : typeof value;
}

function requireString(value: unknown, what: string): string {
  if (typeof value !== 'string' || value.trim() === '') {
    throw new Error(`${what} must be a non-blank string but was ${describe(value)}`);
  }
  return value;
}

function optionalString(value: unknown, what: string): string | undefined {
  if (value === undefined || value === null) {
    return undefined;
  }
  return requireString(value, what);
}

function optionalBoolean(value: unknown, what: string): boolean {
  if (value === undefined || value === null) {
    return false;
  }
  if (typeof value !== 'boolean') {
    throw new Error(`${what} must be a boolean but was ${describe(value)}`);
  }
  return value;
}

function parseArray(value: unknown, what: string): unknown[] {
  if (value === undefined || value === null) {
    return [];
  }
  if (!Array.isArray(value)) {
    throw new Error(`${what} must be an array but was ${describe(value)}`);
  }
  return value;
}

function parseStringArray(value: unknown, what: string): string[] {
  return parseArray(value, what).map((entry, index) => requireString(entry, `${what}[${index}]`));
}

function parseMeta(value: unknown): ScreenMeta {
  const meta = requireObject(value, 'meta');
  return {
    screenId: requireString(meta['screenId'], 'meta.screenId'),
    revisionId: requireString(meta['revisionId'], 'meta.revisionId'),
    recordId: optionalString(meta['recordId'], 'meta.recordId'),
    etag: optionalString(meta['etag'], 'meta.etag'),
    capabilities: parseStringArray(meta['capabilities'], 'meta.capabilities'),
  };
}

function parseLayout(value: unknown): Layout {
  const layout = requireObject(value, 'layout');
  return {
    screenId: requireString(layout['screenId'], 'layout.screenId'),
    recordType: requireString(layout['recordType'], 'layout.recordType'),
    regions: parseArray(layout['regions'], 'layout.regions').map((node, index) =>
      parseNode(node, `layout.regions[${index}]`),
    ),
  };
}

function parseNode(value: unknown, path: string): LayoutNode {
  const node = requireObject(value, path);
  const id = requireString(node['id'], `${path}.id`);
  const kind = requireString(node['kind'], `${path}.kind`);

  switch (kind) {
    case 'REGION':
      return {
        kind: 'REGION',
        id,
        grid: parseGrid(node['grid'], `${path}.grid`),
        children: parseChildren(node['children'], path),
      };
    case 'GROUP':
      return {
        kind: 'GROUP',
        id,
        label: optionalString(node['label'], `${path}.label`),
        collapsible: optionalBoolean(node['collapsible'], `${path}.collapsible`),
        span: parseSpan(node['span'], `${path}.span`),
        grid: parseGrid(node['grid'], `${path}.grid`),
        children: parseChildren(node['children'], path),
      };
    case 'SLOT':
      return {
        kind: 'SLOT',
        id,
        span: parseSpan(node['span'], `${path}.span`),
        component: parseComponent(node['component'], `${path}.component`),
      };
    default:
      // Unlike a component type, an unknown node *kind* is not something a renderer can degrade
      // around: it would have to guess whether the node is a container or a leaf.
      throw new Error(
        `${path}.kind '${kind}' is not a layout node kind; expected REGION, GROUP or SLOT`,
      );
  }
}

function parseChildren(value: unknown, path: string): LayoutNode[] {
  return parseArray(value, `${path}.children`).map((child, index) =>
    parseNode(child, `${path}.children[${index}]`),
  );
}

function parseGrid(value: unknown, path: string): GridSpec | undefined {
  if (value === undefined || value === null) {
    return undefined;
  }
  const grid = requireObject(value, path);
  return {
    columns: parseSpan(grid['columns'], `${path}.columns`) ?? { base: 1 },
    gap: optionalString(grid['gap'], `${path}.gap`),
  };
}

function parseSpan(value: unknown, path: string): SpanMap | undefined {
  if (value === undefined || value === null) {
    return undefined;
  }
  const raw = requireObject(value, path);
  const span: Record<string, number> = {};
  for (const [breakpoint, count] of Object.entries(raw)) {
    if (typeof count !== 'number') {
      throw new Error(`${path}.${breakpoint} must be a number but was ${describe(count)}`);
    }
    span[breakpoint] = count;
  }
  // Reuse the cascade's own validation so the client rejects exactly what the server rejects.
  return normaliseSpan(span as SpanMap);
}

function parseComponent(value: unknown, path: string): ComponentRef {
  const component = requireObject(value, path);
  const options = component['options'];
  return {
    type: requireString(component['type'], `${path}.type`),
    fieldId: optionalString(component['fieldId'], `${path}.fieldId`),
    options: options === undefined || options === null ? {} : requireObject(options, `${path}.options`),
  };
}

function parseFields(value: unknown): Record<string, FieldState> {
  if (value === undefined || value === null) {
    return {};
  }
  const raw = requireObject(value, 'fields');
  const fields: Record<string, FieldState> = {};
  for (const [fieldId, state] of Object.entries(raw)) {
    fields[fieldId] = parseFieldState(state, `fields.${fieldId}`);
  }
  return fields;
}

function parseFieldState(value: unknown, path: string): FieldState {
  const state = requireObject(value, path);
  return {
    type: requireString(state['type'], `${path}.type`),
    label: optionalString(state['label'], `${path}.label`),
    value: state['value'] ?? undefined,
    options: parseArray(state['options'], `${path}.options`).map((option, index) =>
      parseFieldOption(option, `${path}.options[${index}]`),
    ),
    readOnly: optionalBoolean(state['readOnly'], `${path}.readOnly`),
    required: optionalBoolean(state['required'], `${path}.required`),
    hidden: optionalBoolean(state['hidden'], `${path}.hidden`),
    errors: parseStringArray(state['errors'], `${path}.errors`),
  };
}

function parseFieldOption(value: unknown, path: string): FieldOption {
  const option = requireObject(value, path);
  const optionValue = requireString(option['value'], `${path}.value`);
  return { value: optionValue, label: optionalString(option['label'], `${path}.label`) ?? optionValue };
}

function parseAction(value: unknown, index: number): ActionModel {
  const path = `actions[${index}]`;
  const action = requireObject(value, path);
  const enabled = optionalBoolean(action['enabled'], `${path}.enabled`);
  const disabledReason = optionalString(action['disabledReason'], `${path}.disabledReason`);
  if (!enabled && disabledReason === undefined) {
    // The same invariant `ActionModel` enforces server-side: an inert button that cannot explain
    // itself is a usability defect, so the contract refuses to express one.
    throw new Error(`${path} is disabled and must state a disabledReason`);
  }
  return {
    id: requireString(action['id'], `${path}.id`),
    label: requireString(action['label'], `${path}.label`),
    method: requireString(action['method'], `${path}.method`),
    href: requireString(action['href'], `${path}.href`),
    enabled,
    disabledReason,
    confirm: optionalBoolean(action['confirm'], `${path}.confirm`),
  };
}
