import type { ReactNode } from 'react';

import type { PresentationModel } from '@/lib/pm/types';
import {
  PresentationModelView,
  type ActionRequest,
  type RenderOptions,
} from '@/renderer/PresentationModelView';

/**
 * The renderer's entire public surface (ARCHITECTURE.md §7.3).
 *
 * <p>One function, from a presentation model to a tree. It knows about regions, groups, slots,
 * spans and component types — and about no record type, no field name and no business rule. Every
 * screen the platform will ever be configured to show goes through here unchanged, which is what
 * makes the frontend replaceable: swapping React for something else means rewriting this file and
 * the registry, not an application.
 */
export function render(pm: PresentationModel, options: RenderOptions = {}): ReactNode {
  return (
    <PresentationModelView pm={pm} registry={options.registry} onAction={options.onAction} />
  );
}

export type { ActionRequest, RenderOptions };
