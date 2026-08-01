import type { CSSProperties } from 'react';

import { normaliseSpan, spanVariables } from '@/lib/pm/span';
import type { GridSpec, SpanMap } from '@/lib/pm/types';

/**
 * A style object that also carries CSS custom properties. React's `CSSProperties` has no room for
 * them, so the template-literal index signature is added here once rather than cast at every call
 * site.
 */
export type StyleWithVariables = CSSProperties & Record<`--mp-${string}`, string>;

/** Spacing tokens the design system defines. An unrecognised token falls back rather than breaks. */
const GAP_TOKENS: readonly string[] = ['xs', 'sm', 'md', 'lg', 'xl'];
const DEFAULT_GAP = 'md';

/**
 * Column counts for a grid container, one custom property per breakpoint plus the gap token.
 *
 * <p>The gap arrives as a token name, never a length: configuration owns structure and the design
 * system owns appearance, which is what stops configured screens drifting into 200 inconsistent
 * designs.
 */
export function gridStyle(grid: GridSpec | undefined): StyleWithVariables {
  return {
    ...spanVariables(normaliseSpan(grid?.columns), 'cols'),
    '--mp-grid-gap': `var(--mp-space-${gapToken(grid?.gap)})`,
  };
}

/** How many of the parent's columns a node occupies, one custom property per breakpoint. */
export function cellStyle(span: SpanMap | undefined): StyleWithVariables {
  return spanVariables(normaliseSpan(span), 'span');
}

function gapToken(gap: string | undefined): string {
  return gap !== undefined && GAP_TOKENS.includes(gap) ? gap : DEFAULT_GAP;
}

/** Joins the class names that are actually present, so an absent CSS Module key adds nothing. */
export function classNames(...names: (string | undefined | false)[]): string {
  return names.filter((name): name is string => typeof name === 'string' && name !== '').join(' ');
}
