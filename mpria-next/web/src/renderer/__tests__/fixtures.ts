import type {
  ActionModel,
  ComponentRef,
  FieldOption,
  FieldState,
  GridSpec,
  GroupNode,
  LayoutNode,
  PresentationModel,
  RegionNode,
  SlotNode,
  SpanMap,
} from '@/lib/pm/types';

/**
 * Builders for renderer tests.
 *
 * <p>Every identifier here is deliberately meaningless — `fieldA`, `typeX`, `screen-1`. The
 * renderer has no domain knowledge, so its tests must have none either; `domain-vocabulary.guard`
 * fails the build if one leaks in. See `docs/ui-configuration.md` §7.
 */

export function region(
  id: string,
  children: LayoutNode[],
  grid: GridSpec = { columns: { base: 1 } },
): RegionNode {
  return { kind: 'REGION', id, grid, children };
}

export function group(
  id: string,
  children: LayoutNode[],
  overrides: Partial<Omit<GroupNode, 'kind' | 'id' | 'children'>> = {},
): GroupNode {
  return { kind: 'GROUP', id, children, ...overrides };
}

export function slot(id: string, component: ComponentRef, span?: SpanMap): SlotNode {
  return { kind: 'SLOT', id, span, component };
}

export function boundTo(fieldId: string, type = 'TEXT_FIELD', options?: Record<string, unknown>): ComponentRef {
  return { type, fieldId, options };
}

export function field(overrides: Partial<FieldState> = {}): FieldState {
  return {
    type: 'STRING',
    label: 'label.a',
    value: undefined,
    options: [],
    readOnly: false,
    required: false,
    hidden: false,
    errors: [],
    ...overrides,
  };
}

export function option(value: string, label = value): FieldOption {
  return { value, label };
}

export function action(overrides: Partial<ActionModel> = {}): ActionModel {
  return {
    id: 'act-1',
    label: 'label.act1',
    method: 'PUT',
    href: '/api/v1/screens/screen-1/rec-1',
    enabled: true,
    confirm: false,
    ...overrides,
  };
}

export function model(
  regions: LayoutNode[],
  fields: Record<string, FieldState> = {},
  actions: ActionModel[] = [],
): PresentationModel {
  return {
    meta: {
      screenId: 'screen-1',
      revisionId: 'rev-1',
      recordId: 'rec-1',
      etag: '"0"',
      capabilities: ['EDIT'],
    },
    layout: { screenId: 'screen-1', recordType: 'typeX', regions },
    fields,
    actions,
  };
}

/** Reads a CSS custom property off a rendered element, which is where the cascade lands. */
export function cssVariable(element: Element, name: string): string {
  return (element as HTMLElement).style.getPropertyValue(name).trim();
}
