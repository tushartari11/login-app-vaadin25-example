import type { ComponentType, ReactElement } from 'react';

import type { FieldState } from '@/lib/pm/types';
import { FieldFrame, type ControlAttributes } from '@/renderer/components/FieldFrame';
import { MissingBinding } from '@/renderer/components/fallbacks';
import type { FieldComponent, FieldComponentProps } from '@/renderer/registry';

/** What a control needs once the frame has resolved the binding and the accessibility wiring. */
export interface BoundControlProps {
  readonly field: FieldState;
  readonly options: Readonly<Record<string, unknown>>;
  readonly control: ControlAttributes;
}

/**
 * Turns a bare control into a registry entry.
 *
 * <p>Every data-bound component needs the same three things — resolve the binding, refuse it if
 * there is no field state, wrap the control in a labelled frame. Doing that once here means a new
 * field type is genuinely just its control markup, which is what "adding a field type is
 * registering a component" has to mean if it is to stay true.
 */
export function boundComponent(
  displayName: string,
  Control: ComponentType<BoundControlProps>,
): FieldComponent {
  function Bound({ slotId, component, field }: FieldComponentProps): ReactElement {
    if (component.fieldId === undefined || field === undefined) {
      return <MissingBinding component={component} />;
    }
    return (
      <FieldFrame slotId={slotId} fieldId={component.fieldId} field={field}>
        {(control) => (
          <Control field={field} options={component.options ?? {}} control={control} />
        )}
      </FieldFrame>
    );
  }
  Bound.displayName = displayName;
  return Bound;
}

/** Renders a field value into something an input can hold. */
export function asText(value: unknown): string {
  if (value === undefined || value === null) {
    return '';
  }
  if (typeof value === 'string') {
    return value;
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  // Structured values belong to component types that know their own shape. A scalar control shows
  // nothing rather than a stringified map.
  return '';
}

/** Reads a numeric component option, falling back when configuration supplies something else. */
export function numberOption(
  options: Readonly<Record<string, unknown>>,
  name: string,
  fallback: number,
): number {
  const value = options[name];
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}
