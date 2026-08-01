import type { ComponentType } from 'react';

import type { ComponentRef, FieldState } from '@/lib/pm/types';
import {
  Checkbox,
  DateField,
  MediaGallery,
  NumberField,
  RichText,
  Select,
  TextField,
} from '@/renderer/components/fields';

/**
 * The registry: the single point of coupling between configuration and this renderer.
 *
 * <p>`component.type` is the key. Adding a field type is adding an entry; adding a *screen*
 * requires nothing at all. That asymmetry is the acceptance test for the whole server-driven
 * design, and it is why the registry is a plain map rather than a switch buried in the tree walk.
 *
 * <p>`config-service` publishes the same set at `GET /api/v1/components`, which is what lets the
 * admin palette offer exactly what some renderer can draw.
 */
export interface FieldComponentProps {
  /** Unique within the screen; used to derive stable, hydration-safe element ids. */
  readonly slotId: string;
  readonly component: ComponentRef;
  /** Absent for unbound components, and for a binding the model carries no state for. */
  readonly field?: FieldState;
}

export type FieldComponent = ComponentType<FieldComponentProps>;

export type ComponentRegistry = Readonly<Record<string, FieldComponent>>;

export const DEFAULT_COMPONENT_REGISTRY: ComponentRegistry = {
  TEXT_FIELD: TextField,
  RICH_TEXT: RichText,
  NUMBER_FIELD: NumberField,
  DATE_FIELD: DateField,
  CHECKBOX: Checkbox,
  SELECT: Select,
  MEDIA_GALLERY: MediaGallery,
};
